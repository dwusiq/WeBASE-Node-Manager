/**
 * Copyright 2014-2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.webank.webase.node.mgr.base.tools;

import static com.webank.webase.node.mgr.base.properties.ConstantProperties.SSH_DEFAULT_PORT;
import static com.webank.webase.node.mgr.base.properties.ConstantProperties.SSH_DEFAULT_USER;

import java.io.File;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.webank.webase.node.mgr.base.code.ConstantCode;
import com.webank.webase.node.mgr.base.exception.NodeMgrException;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SshTools {

    private static Properties config = new Properties();

    static {
        config.put("StrictHostKeyChecking", "no");
        config.put("CheckHostIP", "no");
        config.put("Compression", "yes");
        config.put("PreferredAuthentications", "publickey");
    }

    public final static String PRIVATE_KEY = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "id_rsa";

    public final static String[] LOCAL_ARRAY = new String[]{"127.0.0.1", "localhost"};

    /**
     * Check ip is local.
     *
     * @param ip
     * @return
     */
    public static boolean isLocal (String ip){
        return Stream.of(LOCAL_ARRAY).anyMatch(ip::equalsIgnoreCase);

    }

    /**
     * @param ip
     * @return
     */
    public static boolean connect(String ip) {
        if (isLocal(ip)){
            return true;
        }

        Session session = connect(ip, SSH_DEFAULT_PORT, SSH_DEFAULT_USER, "", 0);
        if (session != null && session.isConnected()) {
            session.disconnect();
            return true;
        }
        return false;
    }

    /**
     * @param ip
     * @param port
     * @param user
     * @param password
     * @param connectTimeoutInSeconds seconds.
     * @return
     */
    public static Session connect(
            String ip,
            short port,
            String user,
            String password,
            int connectTimeoutInSeconds) {
        if (StringUtils.isBlank(ip)
                || (!"localhost".equals(ip) && !ValidateUtil.validateIpv4(ip))) {
            return null;
        }
        user = StringUtils.isBlank(user) ? SSH_DEFAULT_USER : user;
        port = port <= 0 ? SSH_DEFAULT_PORT : port;
        boolean pubAuth = StringUtils.isBlank(password);

        // set default connect timeout to 10s
        connectTimeoutInSeconds = connectTimeoutInSeconds <= 0 ? 10 : connectTimeoutInSeconds;

        String hostDetail = String.format("[%s@%s:%s] by [%s] with connectTimeout:[%s]",
                user, ip, port, pubAuth ? "public_key" : "password", connectTimeoutInSeconds);
        log.info("Start to connect to host:{} using SSH...", hostDetail);
        JSch jsch = new JSch();
        Session session = null;
        try {
            session = jsch.getSession(user, ip, port);
            session.setConfig(config);
            if (pubAuth) {
                jsch.addIdentity(PRIVATE_KEY);
            } else {
                throw new NodeMgrException(ConstantCode.UNSUPPORTED_PASSWORD_SSH_ERROR);
            }
            session.connect(connectTimeoutInSeconds * 1000);
        } catch (Exception e) {
            log.info("Connect to host:[{}] ERROR!!!", hostDetail, e);
        }
        return session;
    }

}