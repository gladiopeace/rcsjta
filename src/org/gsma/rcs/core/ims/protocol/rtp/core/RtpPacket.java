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

package org.gsma.rcs.core.ims.protocol.rtp.core;

/**
 * Class RtpPacket.
 */
public class RtpPacket extends org.gsma.rcs.core.ims.protocol.rtp.util.Packet {
    /**
     * The base.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.util.Packet base;

    /**
     * The marker.
     */
    public int marker;

    /**
     * The payload type.
     */
    public int payloadType;

    /**
     * The seqnum.
     */
    public int seqnum;

    /**
     * The timestamp.
     */
    public long timestamp;

    /**
     * The ssrc.
     */
    public int ssrc;

    /**
     * The payloadoffset.
     */
    public int payloadoffset;

    /**
     * The payloadlength.
     */
    public int payloadlength;

    /**
     * Creates a new instance of RtpPacket.
     */
    public RtpPacket() {
        super();
    }

    /**
     * Creates a new instance of RtpPacket.
     *  
     * @param arg1 The arg1.
     */
    public RtpPacket(org.gsma.rcs.core.ims.protocol.rtp.util.Packet arg1) {
        super();
    }

    /**
     *  
     * @param arg1 The arg1.
     * @throws IOException if an i/o error occurs
     */
    public void assemble(int arg1) throws java.io.IOException {

    }

    /**
     *  
     * @return  The int.
     */
    public int calcLength() {
        return 0;
    }

} // end RtpPacket
