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
package org.boothub

import com.github.jknack.handlebars.EscapingStrategy
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.helper.StringHelpers
import com.github.jknack.handlebars.io.FileTemplateLoader
import com.github.jknack.handlebars.io.TemplateLoader
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FirstParam
import org.beryx.textio.InputReader
import org.boothub.context.ConfiguredBy
import org.boothub.hbs.Helpers
import org.codehaus.groovy.runtime.IOGroovyMethods
import org.codehaus.groovy.tools.Utilities
import org.yaml.snakeyaml.Yaml

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import static org.boothub.Constants.*

class Util {
    public static final String MAVEN_ID_REGEX = "[A-Za-z0-9_\\-.]+";

    static InputReader.ValueChecker<String> mavenIdChecker = {id, prop ->
        isValidMavenId(id) ? null : ['Not a valid Maven ID']
    }
    static InputReader.ValueChecker<String> packageNameChecker = {pkg, prop ->
        isValidPackageName(pkg) ? null : ['Invalid package name']
    }
    static InputReader.ValueChecker<String> classNameChecker = {name, prop ->
        Utilities.isJavaIdentifier(name) ? null : ['Invalid class name']}


    static Path getPackageAsPath(String pkg) {
        Paths.get(*pkg.split('\\.'))
    }

    static String getFileExtension(String fileName) {
        int pos = fileName.lastIndexOf('.')
        (pos <= 0) ? '' : fileName.substring(pos)
    }

    static deletePathOnExit(Path path) {
        addShutdownHook {
            def f = path.toFile()
            if(f.isFile()) f.delete()
            if(f.isDirectory()) f.deleteDir()
        }
    }

    static Path createTempDirWithDeleteOnExit() {
        def path = Files.createTempDirectory("boothub-")
        deletePathOnExit(path)
        path
    }

    static Path createTempFileWithDeleteOnExit(String suffix) {
        def path = Files.createTempFile("boothub-", suffix)
        deletePathOnExit(path)
        path
    }

    static createDirForPath(Path path, boolean deleteIfExists) {
        def dir = path.toFile()
        if(dir.isFile()) {
            if(!deleteIfExists) throw new IOException("File $path already exists")
            if(!dir.delete()) throw new IOException("Cannot delete file $path")
        }
        if(dir.list()) {
            if(!deleteIfExists) throw new IOException("Directory $path already exists and is not empty")
            if(!dir.deleteDir()) throw new IOException("Cannot delete directory $path")
        }
        if(!dir.isDirectory()) dir.mkdirs()
        if(!dir.isDirectory()) throw new IOException("Cannot create the directory $path")
    }


    static ensureDirExists(Path path) {
        def dir = path.toFile()
        if(dir.isFile()) {
            throw new IOException("$path exists, but it is a file")
        }
        if(!dir.isDirectory()) dir.mkdirs()
        if(!dir.isDirectory()) throw new IOException("Cannot create the directory $path")
    }


    static void downloadFile(String url, Path filePath) {
        ensureDirExists(filePath.parent)
        new FileOutputStream(filePath.toFile()).withStream {it.channel.truncate(0)}
        def conn = new URL(url).openConnection()
        conn.setUseCaches(false)
        conn.setDefaultUseCaches(false)
        conn.setRequestProperty( "Pragma",  "no-cache" );
        conn.setRequestProperty( "Cache-Control",  "no-cache" );
        IOGroovyMethods.withCloseable(conn.getInputStream()) { istream ->
            filePath << istream
        }
    }

    static String getSha256(File file) {
        MessageDigest md = MessageDigest.getInstance("SHA-256")
        FileInputStream istream = new FileInputStream(file)
        byte[] dataBytes = new byte[8192]
        int count
        while ((count = istream.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, count)
        }
        return md.digest().encodeHex().toString()
    }

    static Handlebars createHandlebars(Path basePath) {
        createHandlebars(new FileTemplateLoader(basePath.toFile(), ""))
    }

    static Handlebars createHandlebars(TemplateLoader templateLoader) {
        Handlebars handlebars = new Handlebars()
                .with(EscapingStrategy.NOOP)
                .with(templateLoader)
        Helpers.register(handlebars)
        org.beryx.hbs.Helpers.register(handlebars)
        StringHelpers.register(handlebars)
        handlebars
    }


    static String asJavaClassName(String text, boolean useCamelCase = true, boolean useUnderscore = false) {
        String clsName = asJavaId(text, useCamelCase, useUnderscore)
        if(clsName) {
            clsName = clsName[0].toUpperCase() + clsName.substring(1)
        }
        clsName
    }

    static String asJavaId(String text, boolean useCamelCase = true, boolean useUnderscore = false) {
        boolean startNewPart = false
        boolean first = true
        StringBuilder sb = new StringBuilder()
        def chars = text.trim().chars
        chars.each {ch ->
            if(Character.isJavaIdentifierPart(ch)) {
                if((first && !Character.isJavaIdentifierStart(ch)) || (startNewPart && useUnderscore)) sb << '_'
                sb << (first ? ch.toLowerCase() : (startNewPart ? (useCamelCase ? ch.toUpperCase() : ch.toLowerCase()) : ch))
                startNewPart = false
                first= false
            } else {
                startNewPart = !first
            }
        }
        sb.toString()
    }

    static boolean isValidPackageName(String pkgName) {
        return pkgName && !pkgName.split('\\.').any { !Utilities.isJavaIdentifier(it) }
    }

    static boolean isValidFullyQualifiedClassName(String fullyQualifiedName) {
        if(!fullyQualifiedName) return false
        int pos = fullyQualifiedName.lastIndexOf('.')
        String className = fullyQualifiedName.substring(pos + 1)
        if(!Utilities.isJavaIdentifier(className)) return false
        if(pos < 0) return true
        String pkgName = fullyQualifiedName.substring(0, pos)
        return isValidPackageName(pkgName)
    }

    static isValidMavenId(String id) {
        return id && id.matches(MAVEN_ID_REGEX)
    }

    static InputReader.ValueChecker<String> getMavenIdChecker(String errMsg) {
        return { id, prop -> Util.isValidMavenId(id) ? null : [errMsg] }
    }

    static String stripAll(String s) {
        s.stripIndent()
                .replaceAll('\t', '    ')
                .trim()
                .replaceAll('(?m)\\s*$\\n', '\n')
                .replaceAll('(?m)^\\s*$\\n', '')
    }

    static void unzipStream(InputStream istream, Path destPath, String subDir = null) {
        if(!subDir) subDir = ''
        else if(!subDir.endsWith('/')) subDir += '/'
        ZipInputStream zstream = (istream instanceof ZipInputStream) ? (ZipInputStream)istream : new ZipInputStream(istream)
        IOGroovyMethods.withCloseable(zstream) {
            ZipEntry entry
            while(entry = zstream.nextEntry) {
                if(!entry.directory) {
                    if(entry.name.startsWith(subDir)) {
                        def relName = entry.name - subDir
                        def destFile = destPath.resolve(relName).toFile()
                        destFile.parentFile.mkdirs()
                        destFile << zstream
                    }
                }
            }
        }
    }

    static Map<String, String> getLicenses(Path baseProjectTemplatePath) {
        Map<String, String> licenses = getStandardLicenses()
        licenses.putAll(getExtraLicenses(baseProjectTemplatePath))
        licenses
    }

    static Map<String, String> getStandardLicenses() {
        Map<String, String> licenses = new TreeMap<>()
        def stream = this.getClass().getResourceAsStream(LICENSES_ZIP_RESOURCE_PATH)
        if(stream == null) throw new IOException("Cannot find resource $LICENSES_ZIP_RESOURCE_PATH")
        ZipInputStream zstream = (stream instanceof ZipInputStream) ? (ZipInputStream)stream : new ZipInputStream(stream)
        IOGroovyMethods.withCloseable(zstream) {
            def suffix = "/$LICENSE_YAML_FILE"
            def suffixLen = suffix.length()
            ZipEntry entry
            while(entry = zstream.nextEntry) {
                if(!entry.directory && entry.name.endsWith(suffix)) {
                    def licenseId = entry.name.substring(0, entry.name.length() - suffixLen)
                    if(!licenseId.contains('/')) {
                        def licenseName = new Yaml().load(zstream).licenseName
                        licenses[licenseId] = licenseName
                    }
                }
            }
        }
        licenses
    }

    static Map<String, String> getExtraLicenses(Path baseProjectTemplatePath) {
        Map<String, String> licenses = new TreeMap<>()
        def customLicensesDir = baseProjectTemplatePath.resolve(TEMPLATE_DIR_LICENSES).toFile()
        if(customLicensesDir.directory) {
            customLicensesDir.eachDir { File dir ->
                def licenseId = dir.name
                def yamlFile = new File(dir, LICENSE_YAML_FILE)
                if(yamlFile.isFile()) {
                    def licenseName = new Yaml().load(yamlFile.newInputStream()).licenseName
                    licenses[licenseId] = licenseName
                }
            }
        }
        licenses
    }

    static List<Class<?>> getConfiguredByInterfaces(Class<?> cls) {
        List<Class<?>> result = new ArrayList<>();
        getAllInterfaces(cls, result, {currClass ->
            def configuredBy = currClass.getAnnotation(ConfiguredBy)
            configuredBy && !configuredBy.inheritConfigurators()
        });
        return result;
    }

    static List<Class<?>> getAllInterfaces(Class<?> cls, @ClosureParams(FirstParam.class)  Closure<Boolean> traversalStopper = null) {
        List<Class<?>> result = new ArrayList<>();
        getAllInterfaces(cls, result, traversalStopper);
        return result;
    }

    static void getAllInterfaces(Class<?> cls, List<Class<?>> result, @ClosureParams(FirstParam.class)  Closure<Boolean> traversalStopper = null) {
        if(cls == null) return;
        if(traversalStopper && traversalStopper.call(cls)) return;
        getAllInterfaces(cls.getSuperclass(), result, traversalStopper);
        for(Class<?> intf : cls.getInterfaces()) {
            getAllInterfaces(intf, result, traversalStopper);
            if(!result.contains(intf)) {
                result.add(intf);
            }
        }
    }
}
