/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.core.ims.protocol.rtp.stream;

/**
 * Class DummyPacketSourceStream.
 */
public class DummyPacketSourceStream extends Thread implements ProcessorInputStream {
    /**
     * Constant DUMMY_SOURCE_OPENING_PERIOD.
     */
    public static final int DUMMY_SOURCE_OPENING_PERIOD = 100;

    /**
     * Constant DUMMY_SOURCE_PERIOD.
     */
    public static final int DUMMY_SOURCE_PERIOD = 15000;

    /**
     * Creates a new instance of DummyPacketSourceStream.
     */
    public DummyPacketSourceStream() {
        super();
    }

    public void run() {

    }

    public void close() {

    }

    /**
     *  
     * @return  The buffer.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.util.Buffer read() throws Exception {
        return (org.gsma.rcs.core.ims.protocol.rtp.util.Buffer) null;
    }

    public void open() {

    }

    /**
     * Returns the format.
     *  
     * @return  The format.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.format.Format getFormat() {
        return (org.gsma.rcs.core.ims.protocol.rtp.format.Format) null;
    }

    public void incomingStarted() {

    }

} // end DummyPacketSourceStream