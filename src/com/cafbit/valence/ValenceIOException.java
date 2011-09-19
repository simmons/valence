/*
 * Copyright 2011 David Simmons
 * http://cafbit.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cafbit.valence;

import java.io.IOException;

public class ValenceIOException extends IOException {
    private static final long serialVersionUID = -3123355580007791630L;
    private static final String DEFAULT_MESSAGE =
        "A network error occurred.";

    public ValenceIOException() {
        super(DEFAULT_MESSAGE);
    }

    public ValenceIOException(Throwable cause) {
        super(cause.getMessage());
        initCause(cause);
    }

    public ValenceIOException(String message) {
        super(message);
    }
    public ValenceIOException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }


    @Override
    public String getMessage() {
        String message = super.getMessage();
        Throwable cause = this.getCause();
        if (((message == null) || (message.length()==0)) && (cause != null)) {
            if (cause instanceof java.net.SocketTimeoutException) {
                message = "The connection timed out.";
            } else {
                message = DEFAULT_MESSAGE;
            }
        }
        return message;
    }
}
