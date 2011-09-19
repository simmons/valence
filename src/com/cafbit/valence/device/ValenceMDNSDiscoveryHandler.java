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

package com.cafbit.valence.device;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.cafbit.motelib.discovery.DiscoveryActivity;
import com.cafbit.motelib.discovery.DiscoveryManagerThread;
import com.cafbit.motelib.discovery.MDNSDiscoveryHandler;
import com.cafbit.motelib.model.DeviceClass;
import com.cafbit.netlib.NetUtil;
import com.cafbit.netlib.dns.DNSAnswer;
import com.cafbit.netlib.dns.DNSMessage;

public class ValenceMDNSDiscoveryHandler implements MDNSDiscoveryHandler {

    private static final String INITIAL_QUERY_NAME = "_rfb._tcp.local";

    private DiscoveryManagerThread discoveryManagerThread;
    private Map<String,Set<InetAddress>> addressCache =
        new HashMap<String,Set<InetAddress>>();
    private Map<String,String> ptrCache =
        new HashMap<String,String>();
    private Map<String,DNSAnswer.SRV> srvCache =
        new HashMap<String,DNSAnswer.SRV>();
    private QueryTimer queryTimer;
    private DeviceClass deviceClass;
    private boolean allowIPv6;

    private static class Candidate {
        public String name;
        public boolean srvQueried = false;
        public DNSAnswer.SRV srv = null;
        public Set<InetAddress> addresses = new HashSet<InetAddress>();
        public Candidate(String name) { this.name = name; }
    };
    private Map<String,Candidate> candidateMap =
        new HashMap<String,Candidate>();

    public ValenceMDNSDiscoveryHandler(DiscoveryManagerThread discoveryManagerThread, DeviceClass deviceClass) {
        this.discoveryManagerThread = discoveryManagerThread;
        this.deviceClass = deviceClass;
    }

    @Override
    public void start() {
        allowIPv6 = NetUtil.testIPv6Support();
        queryTimer = new QueryTimer();
        query(INITIAL_QUERY_NAME);
    }

    @Override
    public void handleMessage(DNSMessage message) {

        for (DNSAnswer answer : message.getAnswers()) {
            queryTimer.cancel(answer.name);
            if (answer.data instanceof DNSAnswer.PTR) {
                DNSAnswer.PTR ptr = (DNSAnswer.PTR)answer.data;
                ptrCache.put(answer.name, ptr.name);

                if (answer.name.equals(INITIAL_QUERY_NAME)) {
                    // we have a PTR record for our discovery query...
                    // add a candidate
                    if (! candidateMap.containsKey(ptr.name)) {
                        candidateMap.put(ptr.name, new Candidate(ptr.name));
                    }
                }
            } else if (answer.data instanceof DNSAnswer.SRV) {
                DNSAnswer.SRV srv = (DNSAnswer.SRV)answer.data;
                srvCache.put(answer.name, srv);
            } else if (answer.data instanceof DNSAnswer.A) {
                // this covers AAAA records, too.
                DNSAnswer.A a = (DNSAnswer.A)answer.data;
                if ((!allowIPv6) && (a.address instanceof Inet6Address)) {
                    continue;
                }
                Set<InetAddress> addressCacheSet = addressCache.get(answer.name);
                if (addressCacheSet == null) {
                    addressCacheSet = new HashSet<InetAddress>();
                    addressCache.put(answer.name, addressCacheSet);
                }
                addressCacheSet.add(a.address);
            }
        }

        for (Candidate c : candidateMap.values()) {
            if (! c.srvQueried) {
                if (! srvCache.containsKey(c.name)) {
                    query(c.name);
                    c.srvQueried = true;
                }
            }
            if ((c.srv == null) && (srvCache.containsKey(c.name))) {
                c.srv = srvCache.get(c.name);
            }
            if (c.srv != null) {
                if (! addressCache.containsKey(c.srv.name)) {
                    // query address of the SRV name
                    query(c.srv.name);
                } else {
                    // we have a match!
                    for (InetAddress address : addressCache.get(c.srv.name)) {
                        if (c.addresses.contains(address)) {
                            continue;
                        } else {
                            c.addresses.add(address);
                        }

                        // construct a Device object based on the responses
                        ValenceDevice device = new ValenceDevice(deviceClass);
                        device.address = address.getHostAddress();
                        device.deviceClass = deviceClass;
                        device.port = c.srv.port;
                        device.serverName = c.srv.name;
                        if (device.serverName.endsWith(".local")) {
                            device.serverName = device.serverName.substring(0, device.serverName.length()-6);
                        }

                        // send the Device object upstream
                        discoveryManagerThread.getHandler().sendCommand(new DiscoveryActivity.DeviceCommand(device));
                    }
                }
            }
        }


    }

    @Override
    public void stop() {
        queryTimer.cancel();
    }

    private Set<String> queriedNames = new HashSet<String>();
    private void query(String name) {
        if (queriedNames.contains(name)) {
            return;
        } else {
            queryTimer.query(name);
        }
    }

    //////////

    private class QueryTimer extends Timer {

        private class QueryTask extends TimerTask {
            public String name;
            public long nextDelay;
            public QueryTask(String name, long nextDelay) {
                this.name = name;
                this.nextDelay = nextDelay;
            }
            @Override
            public void run() {
                if (! runningTasks.containsKey(name)) {
                    return;
                }
                // send query
                discoveryManagerThread.ipc.sendMDNSQuery(name);
                // re-queue with exponential backoff
                long thisDelay = nextDelay;
                nextDelay *= 2;

                QueryTask task = new QueryTask(name, nextDelay);
                QueryTimer.this.schedule(task, thisDelay);
            }
        }

        private Map<String,QueryTask> runningTasks =
            Collections.synchronizedMap(
                new HashMap<String,QueryTask>()
            );

        private Random random = new Random(System.currentTimeMillis());
        public QueryTimer() {
            super("valence-mdns-query", true);
        }
        public void query(String name) {
            long initialDelay = 20 + random.nextInt(100);
            long nextDelay = 250;
            QueryTask queryTask = new QueryTask(name, nextDelay);
            runningTasks.put(name, queryTask);
            schedule(queryTask, initialDelay);
        }
        public void cancel(String name) {
            QueryTask queryTask = runningTasks.get(name);
            if (queryTask != null) {
                queryTask.cancel();
                runningTasks.remove(name);
            }
        }
    }

}
