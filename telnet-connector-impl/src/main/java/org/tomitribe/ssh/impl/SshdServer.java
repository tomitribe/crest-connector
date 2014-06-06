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
package org.tomitribe.ssh.impl;

import java.io.File;
import java.io.IOException;

import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.SecurityService;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.tomitribe.telnet.impl.ConsoleSession;
import org.tomitribe.telnet.impl.TtyCodes;

public class SshdServer implements SshdConstants {

    private SshServer sshServer;
    private final ConsoleSession session;
    private final int port;
    private final String domain;

    public SshdServer(ConsoleSession session, String domain) {
        this(session, DEFAULT_PORT, domain);
    }
    
    public SshdServer(ConsoleSession session, int port, String domain) {
        this.session = session;
        this.port = port;
        this.domain = domain;
    }

    public void start() {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(port);
        sshServer.setHost("0.0.0.0");


        if (SecurityUtils.isBouncyCastleRegistered()) {
            sshServer.setKeyPairProvider(new PEMGeneratorHostKeyProvider(new File(BASE_PATH, KEY_NAME + ".pem")
                    .getPath()));
        } else {
            sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File(BASE_PATH, KEY_NAME + ".ser")
                    .getPath()));
        }

        sshServer.setShellFactory(new Factory<Command>() {
            @Override
            public Command create() {
                return new TomEECommands(session);
            }
        });
        
        sshServer.setPasswordAuthenticator(new PasswordAuthenticator() {

            @Override
            public boolean authenticate(String username, String password, ServerSession session) {
                
                boolean validUser = false;
                try {
                    SecurityService securityService = SystemInstance.get().getComponent(SecurityService.class);
                    Object user = securityService.login(domain, username, password);
                    if (user != null) {
                        validUser = true;
                        securityService.logout(user);    
                    }
                } catch (Exception e) {
                    validUser = false;
                }
                
                return validUser;
                
            }
        });

        try {
            sshServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            sshServer.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
