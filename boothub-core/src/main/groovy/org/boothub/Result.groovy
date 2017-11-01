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

import groovy.transform.Canonical
import groovy.util.logging.Slf4j

import java.util.function.Predicate

@Slf4j
@Canonical
class Result<V> {
    static enum Type {SUCCESS, WARNING, ERROR}

    Type type = Type.SUCCESS
    String message
    V value

    public static <W> Result<W> onFailure(String errMessage, Closure<W> valueProvider) {
        try {
            return new Result<W>(type: Type.SUCCESS, value: valueProvider.call())
        } catch (Exception e) {
            log.error(errMessage, e)
            return new Result<W>(type: Type.ERROR, message: errMessage)
        }
    }

    boolean isSuccessful() {
        type == Type.SUCCESS
    }

    V valueOrThrow() {
        if(!successful) throw new RuntimeException(message)
        value
    }

    public <W> Result<W> mapValueIf(Predicate<Result<V>> checker, Closure<W> mapper) {
        if(!checker.test(this)) {
            new Result<W>(type: type, message: message, value: null)
        } else {
            try {
                new Result<W>(type: type, message: message, value: mapper.call(value))
            } catch (Exception e) {
                log.error("Cannot map result: $this", e)
                new Result<W>(type: Type.ERROR, message: "An error occurred.", value: null)
            }
        }
    }

    public <W> Result<W> mapValue(Closure<W> mapper) {
        mapValue({true}, mapper)
    }

    public <W> Result<W> mapValueIfSuccess(Closure<W> mapper) {
        mapValueIf({res -> res.successful}, mapper)
    }

    public <W> Result<W> mapValueIfNotNull(Closure<W> mapper) {
        mapValueIf({res -> res.value != null}, mapper)
    }

    public <W> Result<W> mapValueIfNotNullAndSuccess(Closure<W> mapper) {
        mapValueIf({res -> res.successful && (res.value != null)}, mapper)
    }
}
