/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomee.sheldon.ssh;

import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.common.util.security.bouncycastle.BouncyCastleGeneratorHostKeyProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.tomee.sheldon.commands.factories.CrestComandsFactory;
import org.apache.tomee.sheldon.adapter.SecurityHandler;
import org.apache.tomee.sheldon.authenticator.PasswordAuthenticatorImpl;

import java.io.File;
import java.io.IOException;

public class SshdServer {

    public static final Session.AttributeKey<Credential> CREDENTIAL = new Session.AttributeKey<Credential>();
    private static final String KEY_NAME = "ssh-key";
    private final ConsoleSession session;
    private final int port;
    private final SecurityHandler securityHandler;
    private SshServer sshServer;
    public SshdServer(ConsoleSession session, int port, SecurityHandler securityHandler) {
        this.session = session;
        this.port = port;
        this.securityHandler = securityHandler;
    }

    public void start() {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(port);
        sshServer.setHost("0.0.0.0");

        final String basePath = new File(System.getProperty("user.dir")).getAbsolutePath();
        if (SecurityUtils.isBouncyCastleRegistered()) {
            sshServer.setKeyPairProvider(new BouncyCastleGeneratorHostKeyProvider(new File(basePath, KEY_NAME + ".pem")
                    .toPath()));
        } else {
            sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File(basePath, KEY_NAME + ".ser")));
        }

        sshServer.setShellFactory(new CrestComandsFactory(session, securityHandler));
        sshServer.setPasswordAuthenticator(new PasswordAuthenticatorImpl(securityHandler));

        try {
            sshServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            sshServer.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Credential {
        private final String value;

        public Credential(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
