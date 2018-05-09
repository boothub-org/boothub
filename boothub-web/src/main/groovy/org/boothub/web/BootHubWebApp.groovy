/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.boothub.web

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j
import org.beryx.textio.web.RatpackTextIoApp
import org.beryx.textio.web.WebTextTerminal
import org.boothub.BootHub
import org.boothub.GitHubUtil
import org.boothub.Result
import org.boothub.Result.Type
import org.boothub.Version
import org.boothub.repo.HSQLDBRepoManager
import org.boothub.repo.RepoManager
import org.boothub.repo.SkeletonSearchOptions
import org.kohsuke.github.GitHub
import org.pac4j.core.profile.UserProfile
import org.pac4j.oauth.client.GitHubClient
import org.pac4j.oauth.profile.OAuth20Profile
import ratpack.exec.Blocking
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.func.Factory
import ratpack.handlebars.HandlebarsModule
import ratpack.handling.Chain
import ratpack.handling.Context
import ratpack.pac4j.RatpackPac4j
import ratpack.pac4j.internal.Pac4jSessionKeys
import ratpack.pac4j.internal.RatpackWebContext
import ratpack.session.Session
import ratpack.session.SessionData

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Function

import static org.boothub.Result.Type.*

@Slf4j
class BootHubWebApp {
    static final String DEFAULT_BASE_PATH = System.properties["java.io.tmpdir"]

    static final int DEFAULT_PORT = 4567

    static final String ENV_APP_CFG = "BOOTHUB_WEB_APP_CFG"
    static final String DEFAULT_APP_CFG = "boothub-web-app.cfg"

    static final String OAUTH_CFG = "boothub-oauth.cfg"
    static final String ENV_OAUTH_NAME = "BOOTHUB_OAUTH_NAME"
    static final String ENV_OAUTH_SCOPE = "BOOTHUB_OAUTH_SCOPE"
    static final String ENV_OAUTH_CALLBACK_URL = "BOOTHUB_OAUTH_CALLBACK_URL"
    static final String ENV_OAUTH_KEY = "BOOTHUB_OAUTH_KEY"
    static final String ENV_OAUTH_SECRET = "BOOTHUB_OAUTH_SECRET"

    static final String OAUTH_INFO_CFG = "boothub-info-oauth.cfg"
    static final String ENV_OAUTH_INFO_NAME = "BOOTHUB_OAUTH_INFO_NAME"
    static final String ENV_OAUTH_INFO_CALLBACK_URL = "BOOTHUB_OAUTH_INFO_CALLBACK_URL"
    static final String ENV_OAUTH_INFO_KEY = "BOOTHUB_OAUTH_INFO_KEY"
    static final String ENV_OAUTH_INFO_SECRET = "BOOTHUB_OAUTH_INFO_SECRET"

    static final String ROOT_REDIRECT = (System.getenv('BOOTHUB_ROOT_REDIRECT') ?: 'app')
    static final long CLI_URL_DELAY_MINUTES = (System.getenv('BOOTHUB_CLI_URL_DELAY_MINUTES') ?: '11') as long
    static final long BOT_DELAY_MINUTES = (System.getenv('BOOTHUB_BOT_DELAY_MINUTES') ?: '10') as long
    static final long HOUSEKEEPING_DELAY_MINUTES = (System.getenv('BOOTHUB_HOUSEKEEPING_DELAY_MINUTES') ?: '17') as long
    static final long HOUSEKEEPING_AGE_MINUTES = (System.getenv('BOOTHUB_HOUSEKEEPING_AGE_MINUTES') ?: '120') as long
    static final String BOT_USER = System.getenv('BOOTHUB_BOT_USER')
    static final String BOT_PASSWORD = System.getenv('BOOTHUB_BOT_PASSWORD')

    final RepoManager repoManager

    int port = DEFAULT_PORT

    String outputDirBasePath = DEFAULT_BASE_PATH
    String zipFilesBasePath = DEFAULT_BASE_PATH

    private boolean browserAutoStart

    private String cliDownloadUrl = ''

    private static final JsonSlurper jsonSlurper = new JsonSlurper().setType(JsonParserType.INDEX_OVERLAY);
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Version, new Version.GsonSerializer())
            .setPrettyPrinting()
            .create()

    private static final GitHubClient gitHubClientRepo = createGitHubClientRepo()
    private static final GitHubClient gitHubClientInfo = createGitHubClientInfo()

    BootHubWebApp() {
        String cfgFileName = System.getenv(ENV_APP_CFG) ?: DEFAULT_APP_CFG
        def cfgFile = new File(cfgFileName)
        ConfigObject cfg = null
        if(cfgFile.isFile()) {
            log.debug("Reading configuration file: $cfgFile")
            cfg = new ConfigSlurper().parse(cfgFile.toURI().toURL())
        } else {
            def cfgResource = BootHubWebApp.getClass().getResource("/$cfgFileName")
            if(cfgResource) {
                log.debug("Reading configuration resource: $cfgResource")
                cfg = new ConfigSlurper().parse(cfgResource)
            }
        }
        if(cfg) {
            log.debug("Using configuration: $cfg")
            if(cfg.port) withPort(cfg.port)
            if(cfg.outputDirBasePath) withOutputDirBasePath(cfg.outputDirBasePath)
            if(cfg.zipFilesBasePath) withZipFilesBasePath(cfg.zipFilesBasePath)
            if(cfg.browserAutoStart) withBrowserAutoStart(cfg.browserAutoStart)
        }
        this.repoManager = cfg?.repoManager ?: new HSQLDBRepoManager.Builder().create()
        log.info("""
            Starting BootHubWebApp with:
                port = $port
                outputDirBasePath = $outputDirBasePath
                zipFilesBasePath = $zipFilesBasePath
        """.stripIndent())
    }

    BootHubWebApp withPort(int port) {
        this.port = port
        this
    }

    BootHubWebApp withBrowserAutoStart(boolean browserAutoStart) {
        this.browserAutoStart = browserAutoStart
        this
    }

    BootHubWebApp withOutputDirBasePath(String outputDirBasePath) {
        this.outputDirBasePath = outputDirBasePath ?: DEFAULT_BASE_PATH
        this
    }
    void setOutputDirBasePath(String outputDirBasePath) {
        withOutputDirBasePath(outputDirBasePath)
    }

    BootHubWebApp withZipFilesBasePath(String zipFilesBasePath) {
        this.zipFilesBasePath = zipFilesBasePath ?: DEFAULT_BASE_PATH
        this
    }
    void setZipFilesBasePath(String zipFilesBasePath) {
        withZipFilesBasePath(zipFilesBasePath)
    }

    private static String getZipId(String outputPath) {
        def zipId = outputPath
        if(outputPath) {
            def startPos = outputPath.lastIndexOf(BootHub.ZIP_FILE_PREFIX)
            if(startPos >= 0) {
                zipId = outputPath.substring(startPos + BootHub.ZIP_FILE_PREFIX.length())
            }
            if(zipId.endsWith(BootHub.ZIP_FILE_SUFFIX)) {
                zipId = zipId.substring(0, zipId.length() - BootHub.ZIP_FILE_SUFFIX.length())
            }
        }
        zipId
    }

    void execute() {
        def webTextTerminal = new WebTextTerminal()
        webTextTerminal.setTimeoutNotEmpty(1000)
        webTextTerminal.setTimeoutHasAction(100)
        webTextTerminal.registerUserInterruptHandler({textTerm -> textTerm.abort()}, true)
        WebTextIoExecutor webTextIoExecutor = new WebTextIoExecutor()
                .withPort(port)
                .withBrowserAutoStart(browserAutoStart)
        def app = new RatpackTextIoApp({textIO, runnerData ->
            def resultData = new BootHubWeb(textIO, repoManager, repoManager.repoCache, runnerData)
                    .withOutputDirBasePath(outputDirBasePath)
                    .withZipFilesBasePath(zipFilesBasePath)
                    .execute()
            if(!resultData) {
                textIO.dispose()
            } else {
                if(resultData.outputPath) {
                    resultData.outputPath = getZipId(resultData.outputPath)
                }
                def jsonResultData = gson.toJson(resultData)
                textIO.dispose(jsonResultData)
            }
        }, webTextTerminal)
        .withSessionDataProvider{session ->
            def sessionData = [:]
            session.get(Pac4jSessionKeys.PAC4J_USER_PROFILE)
                    .then {
                        it.ifPresent { UserProfile profile ->
                            sessionData.accessToken = profile.attributes.access_token
                            sessionData.ghUserId = profile.attributes.login
                            sessionData.ghUserName = profile.attributes.name
                        }
                        sessionData.completed = true
                    }
            sessionData
        }

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({ -> updateCliUrl()}, 0, CLI_URL_DELAY_MINUTES, TimeUnit.MINUTES)
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({ -> updateJsonRepo()}, 1, BOT_DELAY_MINUTES, TimeUnit.MINUTES)
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({ -> performHousekeeping()}, HOUSEKEEPING_DELAY_MINUTES, HOUSEKEEPING_DELAY_MINUTES, TimeUnit.MINUTES)

        app.server.bindings << ({binding -> binding.module(HandlebarsModule, {cfg -> cfg.templatesPath('static')})} as Action)

        app.server.handlers << ({ chain ->
            chain
                .path("") {ctx ->
                    ctx.redirect(ROOT_REDIRECT)
                }

                .path("index.html") {ctx ->
                    ctx.redirect(ROOT_REDIRECT)
                }

                .prefix("info") { appChain ->
                    appChain.all(RatpackPac4j.authenticator("callback", gitHubClientInfo))
                    .prefix("auth/login/:route", loginAction(gitHubClientInfo))
                }
                .prefix("app") { appChain ->
                    appChain.all(RatpackPac4j.authenticator("callback", gitHubClientRepo))
                    .path("") {ctx ->
                        Session session = ctx.get(Session)
                        session.data.then{sessionData ->
                            def model = getModel(sessionData)
                            sessionData.set("autoRun", false)
                            ctx.render(Paths.get(BootHubWebApp.getClass().getResource("/static/app.html").toURI()))
                        }
                    }
                    .prefix("auth") { authChain ->
                        authChain.path("logout") { ctx ->
                            def route = ctx.pathTokens.route ?: 'home'
                            log.debug("route: $route")
                            ctx.get(Session).data.then { sessionData ->
                                log.info("sessionData before: $sessionData.keys")
                                sessionData.clear()
                                log.info("sessionData after: $sessionData.keys")
                            }
                            RatpackWebContext.from(ctx, false).then{ webContext ->
                                log.info("webContext.session before: $webContext.session.keys")
                                webContext.session.clear()
                                log.info("webContext.session after: $webContext.session.keys")
                            }
                            RatpackPac4j.logout(ctx).then { -> ctx.render("You have been signed out") }
                        }
                        .prefix("login/:route", loginAction(gitHubClientRepo))
                    }
                }
                .prefix("zip/:fName") { zipChain ->
                    zipChain
                        .all{ ctx ->
                            def fName = ctx.pathTokens.fName
                            log.debug("fname: $fName")
                            def zipFilePath = Paths.get("$zipFilesBasePath/${BootHub.ZIP_FILE_PREFIX}${fName}${BootHub.ZIP_FILE_SUFFIX}")
                            log.debug("zipFilePath: $zipFilePath")
                            def fileId = ctx.request.queryParams.ghProjectId ?: "project"
                            ctx.response.contentType("application/zip")
                            ctx.response.headers.add("Content-Disposition", "attachment; filename=\"${fileId}.zip\"")
                            ctx.render(zipFilePath)
                        }
                }

                .path("state") { ctx ->
                    Session session = ctx.get(Session)
                    session.data.then { sessionData ->
                        def stateMap = getModel(sessionData)
                        ctx.render(gson.toJson(stateMap))
                    }
                }

            .prefix("api") { apiChain -> apiChain
                    .get("cliDownloadUrl") { ctx -> renderSuccessValue(ctx, cliDownloadUrl) }

//########################################################
//#############  REPO MANAGER  ###########################
                    .get("skeletons") { ctx ->
                        def searchOpts = SkeletonSearchOptions.fromParameterMap(ctx.request.queryParams)
                        handleQuerySkeletons(ctx, searchOpts)
                    }

                    .post("querySkeletons") { ctx ->
                        ctx.request.body.map{it.text}.then{ bodyText ->
                            log.debug("bodyText: $bodyText")
                            def searchOpts = jsonSlurper.parseText(bodyText) as SkeletonSearchOptions
                            handleQuerySkeletons(ctx, searchOpts)
                        }
                    }

                    .post("addSkeleton") { ctx -> handleAddSkeleton(ctx) }
                    .post("deleteSkeleton") { ctx -> handleDeleteSkeleton(ctx) }
                    .post("deleteSkeletonEntry") { ctx -> handleDeleteSkeletonEntry(ctx) }
                    .post("queryTags") { ctx -> handleQueryTags(ctx) }
                    .post("addTag") { ctx -> handleAddTag(ctx) }
                    .post("deleteTag") { ctx -> handleDeleteTag(ctx) }
                    .post("queryOwners") { ctx -> handleQueryOwners(ctx) }
                    .post("addOwner") { ctx -> handleAddOwner(ctx) }
                    .post("deleteOwner") { ctx -> handleDeleteOwner(ctx) }
            }

        } as Action)
        webTextIoExecutor.execute(app)
    }


    private Action<Chain> loginAction(GitHubClient gitHubClient) {
        return { loginChain ->
            loginChain.all(RatpackPac4j.requireAuth(GitHubClient))
            .all { ctx ->
                def route = ctx.pathTokens.route ?: 'home'
                log.info("initial route: $route")
                switch(route) {
                    case 'home':
                        def skeletonUrl = ctx.request.queryParams.skeletonUrl
                        def exec = Boolean.parseBoolean(ctx.request.queryParams.exec)
                        if(skeletonUrl || exec) {
                            route += "/$exec"
                            if(skeletonUrl) {
                                route += "/${URLEncoder.encode(skeletonUrl, StandardCharsets.UTF_8.name())}"
                            }
                        }
                        break;
                    default:
                        break;
                }
                log.debug("Redirecting login to: $route")
                ctx.redirect("/app#/$route")
            }
        }
    }

    private void updateJsonRepo() {
        try {
            log.debug("Start updating boothub-repo")
            if(!BOT_USER || !BOT_PASSWORD) {
                log.warn("updateJsonRepo(): bot credentials not set.")
                return
            }
            def result = repoManager.getSkeletons(SkeletonSearchOptions.ALL_COMPACT)
            if(!result.successful) {
                log.warn("updateJsonRepo(): getSkeletons() returned: $result")
                return
            }
            def jsonText = gson.toJson(result.value)

            def ghApi = GitHubUtil.connectUsingPassword(BOT_USER, BOT_PASSWORD)
            def ghRepo = ghApi.getRepository('boothub-org/boothub-repo')
            if(!ghRepo) {
                log.warn("updateJsonRepo(): cannot retrieve boothub-repo")
                return
            }
            boolean updated = GitHubUtil.updateContent(ghRepo, jsonText, "updated by $BOT_USER", 'repo.json', false)
            if(updated) {
                log.info("boothub-repo updated")
            }
        } catch (Exception e) {
            log.error("Failed to update boothub-repo.", e)
        }
    }

    private void performHousekeeping() {
        try {
            log.debug("Start performing housekeeping")
            String tmpDir = System.properties['java.io.tmpdir'] ?: '/tmp'
            long maxTime = System.currentTimeMillis() - 60000L * HOUSEKEEPING_AGE_MINUTES
            File[] toDelete = new File(tmpDir).listFiles({f ->
                if(!f.name.startsWith('boothub-')) return false
                if(f.name.startsWith('boothub-cache')) return false
                return (f.lastModified() < maxTime)
            } as FileFilter)
            if(toDelete) {
                toDelete.each {f ->
                    boolean deleted = f.file ? f.delete() : f.directory ? f.deleteDir() : false
                    log.info("Housekeeping: $f deleted: $deleted")
                }
            }
        } catch (Exception e) {
            log.error("Failed to perform housekeeping.", e)
        }
    }

    private void updateCliUrl() {
        def url = retrieveCliUrl()
        log.info("CLI download URL: $url")
        if(url) cliDownloadUrl = url
    }

    private String retrieveCliUrl() {
        try {
            def github = GitHub.connectAnonymously()
            def repo = github.getRepository('boothub-org/boothub')
            def zipAsset = {it.name.matches("boothub-.+\\.zip")}

            def asset = repo.latestRelease.assets.find(zipAsset)
            if(asset) return asset.browserDownloadUrl

            log.warn("Latest release has no downloadable zip.")

            repo.listReleases().find {it.assets.find {it.name.matches("boothub-.+\\.zip")}}
            for(def rel : repo.listReleases()) {
                asset = rel.assets.find(zipAsset)
                if(asset) return asset.browserDownloadUrl
            }
            null
        } catch (Exception e) {
            log.error("Failed to retrieve the CLI URL.", e)
        }
    }

    private void handleQuerySkeletons(Context ctx, SkeletonSearchOptions searchOpts) {
        log.debug("searchOptions: $searchOpts")
        handleResultFactory(ctx) { repoManager.getSkeletons(searchOpts) }
    }

    private void handleAddSkeleton(Context ctx) {
        withAuthenticatedUser(ctx) { parameters, userId ->
            def url = parameters.url
            handleResultFactory(ctx) { repoManager.addSkeleton(url, userId) }
        }
    }

    private void handleDeleteSkeleton(Context ctx) {
        handleDataManipulation(ctx,
            { prm -> "Skeleton $prm.skeletonId not found."},
            { parameters ->
                def skeletonId = parameters.skeletonId
                repoManager.deleteSkeleton(skeletonId)
            })
    }

    private void handleDeleteSkeletonEntry(Context ctx) {
        handleDataManipulation(ctx,
            { prm -> "Skeleton $prm.skeletonId version $prm.version not found."},
            { parameters ->
                def skeletonId = parameters.skeletonId
                def version = parameters.version
                repoManager.deleteEntry(skeletonId, version)
            })
    }

    private void handleQueryTags(Context ctx) {
        withParameters(ctx) { parameters ->
            def skeletonId = parameters.skeletonId
            handleResultFactory(ctx) { repoManager.getTags(skeletonId) }
        }
    }

    private void handleAddTag(Context ctx) {
        handleDataManipulation(ctx,
            { prm -> "Tag $prm.tag of $prm.skeletonId already exists."},
            { parameters ->
                def skeletonId = parameters.skeletonId
                def tag = parameters.tag
                repoManager.addTag(skeletonId, tag)
            })
    }

    private void handleDeleteTag(Context ctx) {
        handleDataManipulation(ctx,
            { prm -> "Tag $prm.tag of $prm.skeletonId not found."},
            { parameters ->
                def skeletonId = parameters.skeletonId
                def tag = parameters.tag
                repoManager.deleteTag(skeletonId, tag)
            })
    }

    private void handleQueryOwners(Context ctx) {
        withParameters(ctx) { parameters ->
            def skeletonId = parameters.skeletonId
            handleResultFactory(ctx) { repoManager.getOwners(skeletonId) }
        }
    }

    private void handleAddOwner(Context ctx) {
        handleDataManipulation(ctx,
                { prm -> "User $prm.ownerId is already owner of skeleton $prm.skeletonId."},
                { parameters ->
                    String skeletonId = parameters.skeletonId
                    String ownerId = parameters.ownerId
                    repoManager.addOwner(skeletonId, ownerId)
                })
    }

    private void handleDeleteOwner(Context ctx) {
        handleDataManipulation(ctx,
                { prm -> "User $prm.ownerId is not an owner of $prm.skeletonId."},
                { parameters ->
                    String skeletonId = parameters.skeletonId
                    String ownerId = parameters.ownerId
                    repoManager.deleteOwner(skeletonId, ownerId)
                })
    }


    private void handleDataManipulation(Context ctx, Function<Map, String> noUpdateMessageProvider,
                        @ClosureParams(value=SimpleType, options="java.util.Map") Closure<Result> handler) {
        withOwnerCheck(ctx) { Map parameters, String userId ->
            safeBlocking(ctx) { handler.call(parameters) }
            .then { Result result ->
                if(!result.successful) renderResult(ctx, result)
                else {
                    int count = result.value
                    if(count > 0) {
                        renderSuccess(ctx)
                    } else {
                        renderWarningMessage(ctx, noUpdateMessageProvider.apply(parameters))
                    }
                }
            }
        }
    }

    private void withOwnerCheck(Context ctx,
                    @ClosureParams(value=SimpleType, options="java.util.Map, java.lang.String") Closure<Void> handler) {
        withAuthenticatedUser(ctx) { Map parameters, String userId ->
            def skeletonId = parameters.skeletonId
            safeBlocking(ctx) { repoManager.getOwners(skeletonId) }
            .then { Result ownersResult ->
                if(!ownersResult.successful) renderResult(ctx, ownersResult)
                else {
                    def owners = ownersResult.value
                    if(!(userId in owners)) {
                        renderErrorMessage(ctx, "User $userId is not an owner of skeleton $skeletonId.")
                    } else {
                        handler.call(parameters, userId)
                    }
                }
            }
        }
    }

    private void withAuthenticatedUser(Context ctx,
                          @ClosureParams(value=SimpleType, options="java.util.Map, java.lang.String") Closure<Void> handler) {
        ctx.get(Session).data.then { sessionData ->
            def userId = getModel(sessionData)?.loggedInUserId
            if (!userId) {
                renderErrorMessage(ctx, "You must be signed in to perform this operation.")
            } else {
                withParameters(ctx) { parameters -> handler.call(parameters, userId)}
            }
        }
    }

    private static void withParameters(Context ctx, @ClosureParams(value=SimpleType, options="java.util.Map") Closure<Void> handler) {
        ctx.request.body.map { body -> jsonSlurper.parseText(body.text) as Map }.
        then { parameters -> handler.call(parameters) }
    }

    private static <V> Promise<Result<V>> handleResultFactory(Context ctx, Factory<Result<V>> factory) {
        safeBlocking(ctx) { factory.create() }
        .then{result -> renderResult(ctx, result)}
    }

    private static <T> Promise<T> safeBlocking(Context ctx, Factory<T> factory) {
        Blocking.get {
            factory.create()
        }
        .onError { throwable ->
            log.error("Operation failed.", throwable)
            renderErrorMessage(ctx, "Operation failed.")
        }
    }

    private static <V> void renderResult(Context ctx, Result result) {
        ctx.render(gson.toJson(result))
    }
    private static <V> void renderResult(Context ctx, Type resultType, String message, V value) {
        renderResult(ctx, new Result(type: resultType, message: message, value: value))
    }
    private static void renderSuccess(Context ctx) {
        renderResult(ctx, SUCCESS, null, null)
    }
    private static <V> void renderSuccessValue(Context ctx, V value) {
        renderResult(ctx, SUCCESS, null, value)
    }
    private static void renderWarningMessage(Context ctx, String message) {
        renderResult(ctx, WARNING, message, null)
    }
    private static void renderErrorMessage(Context ctx, String message) {
        renderResult(ctx, ERROR, message, null)
    }

    private Map<String,?> getModel(SessionData sessionData) {
        Map<String,?> model = [:]
        sessionData.get(Pac4jSessionKeys.PAC4J_USER_PROFILE).ifPresent { UserProfile profile ->
            if(profile instanceof OAuth20Profile) {
                OAuth20Profile oAuth20Profile = profile;
                model.loggedInUserId = oAuth20Profile.username
                model.loggedInDisplayName = oAuth20Profile.displayName
                model.loggedInPictureUrl = oAuth20Profile.pictureUrl
                model.loggedInProfileUrl = oAuth20Profile.profileUrl
                model.loggedInInfoOnly = profile.clientName != gitHubClientRepo.name
            }
        }
        model += sessionData.keys
                .findAll {key -> !(key.name in [Pac4jSessionKeys.PAC4J_USER_PROFILE])}
                .collectEntries {key ->
                    def value = sessionData.get(key)
                    if(value instanceof Optional) {
                        value = ((Optional)value).orElse("")
                    }
                    [key.name, value]
                }
                .findAll {k,v -> v}
        log.debug("model: $model")
        model
    }

    private static GitHubClient createGitHubClientRepo() {
        def oauthCfg = BootHubWebApp.getClass().getResource("/$OAUTH_CFG")
        ConfigObject cfg = oauthCfg ? new ConfigSlurper().parse(oauthCfg) : null

        def ghClient = new GitHubClient()
        ghClient.name = System.getenv(ENV_OAUTH_NAME) ?: cfg?.name ?: 'BootHub'
        ghClient.scope = System.getenv(ENV_OAUTH_SCOPE) ?: cfg?.scope ?: 'public_repo read:org'
        ghClient.callbackUrl = System.getenv(ENV_OAUTH_CALLBACK_URL) ?: cfg?.callbackUrl
        ghClient.key = System.getenv(ENV_OAUTH_KEY) ?: cfg?.key
        ghClient.secret = System.getenv(ENV_OAUTH_SECRET) ?: cfg?.secret
        ghClient
    }

    private static GitHubClient createGitHubClientInfo() {
        def oauthCfg = BootHubWebApp.getClass().getResource("/$OAUTH_INFO_CFG")
        ConfigObject cfg = oauthCfg ? new ConfigSlurper().parse(oauthCfg) : null

        def ghClient = new GitHubClient()
        ghClient.name = System.getenv(ENV_OAUTH_INFO_NAME) ?: cfg?.name ?: 'BootHub'
        ghClient.scope = ''
        ghClient.callbackUrl = System.getenv(ENV_OAUTH_INFO_CALLBACK_URL) ?: cfg?.callbackUrl
        ghClient.key = System.getenv(ENV_OAUTH_INFO_KEY) ?: cfg?.key
        ghClient.secret = System.getenv(ENV_OAUTH_INFO_SECRET) ?: cfg?.secret
        ghClient
    }

    static void main(String[] args) {
        new BootHubWebApp().execute()
    }
}
