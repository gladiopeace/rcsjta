/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to interface the message table
 */
public class MessageLog implements IMessageLog {

    private LocalContentResolver mLocalContentResolver;

    private GroupChatLog mGroupChatLog;

    private GroupDeliveryInfoLog mGroupChatDeliveryInfoLog;

    private final RcsSettings mRcsSettings;

    private static final Logger sLogger = Logger.getLogger(MessageLog.class.getSimpleName());

    private static final String[] PROJECTION_MESSAGE_ID = new String[] {
        MessageData.KEY_MESSAGE_ID
    };

    private static final String[] PROJECTION_GROUP_CHAT_EVENTS = new String[] {
            MessageData.KEY_STATUS, MessageData.KEY_CONTACT
    };

    private static final String SELECTION_GROUP_CHAT_EVENTS = new StringBuilder(
            MessageData.KEY_CHAT_ID).append("=? AND ").append(MessageData.KEY_MIME_TYPE)
            .append("='").append(MimeType.GROUPCHAT_EVENT).append("' GROUP BY ")
            .append(MessageData.KEY_CONTACT).toString();

    private static final int FIRST_COLUMN_IDX = 0;

    private static final String SELECTION_QUEUED_ONETOONE_CHAT_MESSAGES = new StringBuilder(
            MessageData.KEY_CHAT_ID).append("=? AND ").append(MessageData.KEY_STATUS).append("=")
            .append(Status.QUEUED.toInt()).toString();

    private static final String SELECTION_QUEUED_GROUP_CHAT_MESSAGES = new StringBuilder(
            MessageData.KEY_CHAT_ID).append("=? AND ").append(MessageData.KEY_STATUS).append("=")
            .append(Status.QUEUED.toInt()).toString();

    private static final String SELECTION_BY_MULTIPLE_MSG_IDS = new StringBuilder(
            MessageData.KEY_MESSAGE_ID).append(" IN(").append("=?)").toString();

    private static final int CHAT_MESSAGE_DELIVERY_EXPIRED = 1;

    private static final int CHAT_MESSAGE_DELIVERY_EXPIRATION_NOT_APPLICABLE = 0;

    private static final String SELECTION_BY_UNDELIVERED_ONETOONE_CHAT_MESSAGES = new StringBuilder(
            MessageData.KEY_EXPIRED_DELIVERY).append("<>").append(CHAT_MESSAGE_DELIVERY_EXPIRED)
            .append(" AND ").append(MessageData.KEY_DELIVERY_EXPIRATION).append("<>")
            .append(CHAT_MESSAGE_DELIVERY_EXPIRATION_NOT_APPLICABLE).append(" AND ")
            .append(MessageData.KEY_STATUS).append(" NOT IN(").append(Status.DELIVERED.toInt())
            .append(",").append(Status.DISPLAYED.toInt()).append(")").toString();

    private static final String ORDER_BY_TIMESTAMP_ASC = MessageData.KEY_TIMESTAMP.concat(" ASC");

    /**
     * Constructor
     * 
     * @param localContentResolver Local content resolver
     * @param groupChatLog
     * @param groupChatDeliveryInfoLog
     * @param rcsSettings
     */
    /* package private */MessageLog(LocalContentResolver localContentResolver,
            GroupChatLog groupChatLog, GroupDeliveryInfoLog groupChatDeliveryInfoLog,
            RcsSettings rcsSettings) {
        mLocalContentResolver = localContentResolver;
        mGroupChatLog = groupChatLog;
        mGroupChatDeliveryInfoLog = groupChatDeliveryInfoLog;
        mRcsSettings = rcsSettings;
    }

    private void addIncomingOneToOneMessage(ChatMessage msg, Status status, ReasonCode reasonCode) {
        ContactId contact = msg.getRemoteContact();
        String msgId = msg.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add incoming chat message: contact=").append(contact)
                    .append(", msg=").append(msgId).append(", status=").append(status)
                    .append(", reasonCode=").append(reasonCode).append(".").toString());
        }

        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, contact.toString());
        values.put(MessageData.KEY_MESSAGE_ID, msgId);
        values.put(MessageData.KEY_CONTACT, contact.toString());
        values.put(MessageData.KEY_DIRECTION, Direction.INCOMING.toInt());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(msg.getMimeType());
        values.put(MessageData.KEY_MIME_TYPE, apiMimeType);
        values.put(MessageData.KEY_CONTENT, ChatUtils.networkContentToPersistedContent(msg));

        values.put(MessageData.KEY_TIMESTAMP, msg.getTimestamp());
        values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getTimestampSent());
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);

        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        mLocalContentResolver.insert(MessageData.CONTENT_URI, values);
    }

    /**
     * Add outgoing one-to-one chat message
     * 
     * @param msg Chat message
     * @param status Status
     * @param reasonCode Reason code
     * @param deliveryExpiration
     */
    @Override
    public void addOutgoingOneToOneChatMessage(ChatMessage msg, Status status,
            ReasonCode reasonCode, long deliveryExpiration) {
        ContactId contact = msg.getRemoteContact();
        String msgId = msg.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add outgoing chat message: contact=").append(contact)
                    .append(", msg=").append(msgId).append(", status=").append(status)
                    .append(", reasonCode=").append(reasonCode).append(".").toString());
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, contact.toString());
        values.put(MessageData.KEY_MESSAGE_ID, msgId);
        values.put(MessageData.KEY_CONTACT, contact.toString());
        values.put(MessageData.KEY_DIRECTION, Direction.OUTGOING.toInt());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(msg.getMimeType());
        values.put(MessageData.KEY_MIME_TYPE, apiMimeType);
        values.put(MessageData.KEY_CONTENT, ChatUtils.networkContentToPersistedContent(msg));

        values.put(MessageData.KEY_TIMESTAMP, msg.getTimestamp());
        values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getTimestampSent());
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, deliveryExpiration);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        mLocalContentResolver.insert(MessageData.CONTENT_URI, values);
    }

    @Override
    public void addOneToOneSpamMessage(ChatMessage msg) {
        addIncomingOneToOneMessage(msg, Status.REJECTED, ReasonCode.REJECTED_SPAM);
    }

    /**
     * Add incoming one-to-one chat message
     * 
     * @param msg Chat message
     * @param imdnDisplayedRequested Indicates whether IMDN display was requested
     */
    @Override
    public void addIncomingOneToOneChatMessage(ChatMessage msg, boolean imdnDisplayedRequested) {
        if (imdnDisplayedRequested) {
            addIncomingOneToOneMessage(msg, Status.DISPLAY_REPORT_REQUESTED, ReasonCode.UNSPECIFIED);

        } else {
            addIncomingOneToOneMessage(msg, Status.RECEIVED, ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Add incoming group chat message
     * 
     * @param chatId Chat ID
     * @param msg Chat message
     * @param imdnDisplayedRequested Indicates whether IMDN display was requested
     */
    @Override
    public void addIncomingGroupChatMessage(String chatId, ChatMessage msg,
            boolean imdnDisplayedRequested) {
        Status chatMessageStatus = imdnDisplayedRequested ? Status.DISPLAY_REPORT_REQUESTED
                : Status.RECEIVED;
        addGroupChatMessage(chatId, msg, Direction.INCOMING, chatMessageStatus,
                ReasonCode.UNSPECIFIED);
    }

    /**
     * Add outgoing group chat message
     * 
     * @param chatId Chat ID
     * @param msg Chat message
     * @param status Status
     * @param reasonCode Reason code
     */
    @Override
    public void addOutgoingGroupChatMessage(String chatId, ChatMessage msg, Status status,
            ReasonCode reasonCode) {
        addGroupChatMessage(chatId, msg, Direction.OUTGOING, status, reasonCode);
    }

    /**
     * Add group chat message
     * 
     * @param chatId Chat ID
     * @param msg Chat message
     * @param direction Direction
     * @param status Status
     * @param reasonCode Reason code
     */
    private void addGroupChatMessage(String chatId, ChatMessage msg, Direction direction,
            Status status, ReasonCode reasonCode) {
        String msgId = msg.getMessageId();
        ContactId contact = msg.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add group chat message; chatId=").append(chatId)
                    .append(", msg=").append(msgId).append(", dir=").append(direction)
                    .append(", contact=").append(contact).append(".").toString());
        }

        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, chatId);
        values.put(MessageData.KEY_MESSAGE_ID, msgId);
        if (contact != null) {
            values.put(MessageData.KEY_CONTACT, contact.toString());
        }
        values.put(MessageData.KEY_DIRECTION, direction.toInt());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(msg.getMimeType());
        values.put(MessageData.KEY_MIME_TYPE, apiMimeType);
        values.put(MessageData.KEY_CONTENT, ChatUtils.networkContentToPersistedContent(msg));
        values.put(MessageData.KEY_TIMESTAMP, msg.getTimestamp());
        values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getTimestampSent());
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        mLocalContentResolver.insert(MessageData.CONTENT_URI, values);

        if (direction == Direction.OUTGOING) {
            try {
                GroupDeliveryInfo.Status deliveryStatus = GroupDeliveryInfo.Status.NOT_DELIVERED;
                if (mRcsSettings.isAlbatrosRelease()) {
                    deliveryStatus = GroupDeliveryInfo.Status.UNSUPPORTED;
                }

                Set<ContactId> recipients = new HashSet<ContactId>();
                for (Map.Entry<ContactId, ParticipantStatus> participant : mGroupChatLog
                        .getParticipants(chatId).entrySet()) {
                    switch (participant.getValue()) {
                        case INVITE_QUEUED:
                        case INVITING:
                        case INVITED:
                        case CONNECTED:
                        case DISCONNECTED:
                            recipients.add(participant.getKey());
                        default:
                            break;
                    }
                }

                for (ContactId recipient : recipients) {
                    /* Add entry with delivered and displayed timestamps set to 0. */
                    mGroupChatDeliveryInfoLog.addGroupChatDeliveryInfoEntry(chatId, recipient,
                            msgId, deliveryStatus, GroupDeliveryInfo.ReasonCode.UNSPECIFIED, 0, 0);
                }
            } catch (Exception e) {
                // TODO CR037 we should not do such rollback nor catch exception at all?
                mLocalContentResolver.delete(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                        null, null);
                mLocalContentResolver.delete(
                        Uri.withAppendedPath(GroupDeliveryInfoData.CONTENT_URI, msgId), null, null);
                /* TODO: Throw exception */
                if (sLogger.isActivated()) {
                    sLogger.warn("Group chat message with msgId '" + msgId
                            + "' could not be added to database!");
                }
            }
        }
    }

    @Override
    public String addGroupChatEvent(String chatId, ContactId contact, GroupChatEvent.Status status,
            long timestamp) {
        if (sLogger.isActivated()) {
            sLogger.debug("Add group chat system message: chatID=" + chatId + ", contact="
                    + contact + ", status=" + status);
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, chatId);
        if (contact != null) {
            values.put(MessageData.KEY_CONTACT, contact.toString());
        }
        String msgId = IdGenerator.generateMessageID();
        values.put(MessageData.KEY_MESSAGE_ID, msgId);
        values.put(MessageData.KEY_MIME_TYPE, MimeType.GROUPCHAT_EVENT);
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, ReasonCode.UNSPECIFIED.toInt());
        values.put(MessageData.KEY_DIRECTION, Direction.IRRELEVANT.toInt());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(MessageData.KEY_TIMESTAMP, timestamp);
        values.put(MessageData.KEY_TIMESTAMP_SENT, timestamp);
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        mLocalContentResolver.insert(MessageData.CONTENT_URI, values);
        return msgId;
    }

    @Override
    public void markMessageAsRead(String msgId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Marking chat message as read: msgId=").append(msgId)
                    .toString());
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.READ.toInt());

        if (mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, null, null) < 1) {
            /* TODO: Throw exception */
            if (sLogger.isActivated()) {
                sLogger.warn("There was no message with msgId '" + msgId + "' to mark as read.");
            }
        }
    }

    /**
     * Set chat message status and reason code. Note that this method should not be used for
     * Status.DELIVERED and Status.DISPLAYED. These states require timestamps and should be set
     * through setChatMessageStatusDelivered and setChatMessageStatusDisplayed respectively.
     * 
     * @param msgId Message ID
     * @param status Message status (See restriction above)
     * @param reasonCode Message status reason code
     */
    @Override
    public void setChatMessageStatusAndReasonCode(String msgId, Status status, ReasonCode reasonCode) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Update chat message: msgId=").append(msgId)
                    .append(", status=").append(status).append(", reasonCode=").append(reasonCode)
                    .toString());
        }

        switch (status) {
            case DELIVERED:
            case DISPLAYED:
                throw new IllegalArgumentException(new StringBuilder("Status that requires ")
                        .append("timestamp passed, use specific method taking timestamp")
                        .append(" to set status ").append(status.toString()).toString());
            default:
        }

        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());

        if (mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, null, null) < 1) {
            /* TODO: Throw exception */
            if (sLogger.isActivated()) {
                sLogger.warn("There was no message with msgId '" + msgId
                        + "' to update status for.");
            }
        }
    }

    @Override
    public void markIncomingChatMessageAsReceived(String msgId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder(
                    "Mark incoming chat message status as received for msgId=").append(msgId)
                    .toString());
        }
        setChatMessageStatusAndReasonCode(msgId, Status.RECEIVED, ReasonCode.UNSPECIFIED);
    }

    @Override
    public boolean isMessagePersisted(String msgId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(MessageData.CONTENT_URI, msgId), PROJECTION_MESSAGE_ID,
                    null, null, null);
            // TODO check null cursor CR037
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Cursor getMessageData(String columnName, String msgId) throws SQLException {
        String[] projection = new String[] {
            columnName
        };
        Cursor cursor = mLocalContentResolver.query(
                Uri.withAppendedPath(MessageData.CONTENT_URI, msgId), projection, null, null, null);
        // TODO check null cursor CR037
        if (cursor.moveToFirst()) {
            return cursor;
        }
        throw new SQLException("No row returned while querying for message data with msgId : "
                + msgId);
    }

    private int getDataAsInt(Cursor cursor) {
        try {
            return cursor.getInt(FIRST_COLUMN_IDX);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private long getDataAsLong(Cursor cursor) {
        try {
            return cursor.getLong(FIRST_COLUMN_IDX);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String getDataAsString(Cursor cursor) {
        try {
            return cursor.getString(FIRST_COLUMN_IDX);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean getDataAsBoolean(Cursor cursor) {
        try {
            return cursor.getInt(FIRST_COLUMN_IDX) == 1;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public boolean isMessageRead(String msgId) {
        return (getDataAsInt(getMessageData(MessageData.KEY_READ_STATUS, msgId)) == ReadStatus.READ
                .toInt());
    }

    @Override
    public long getMessageSentTimestamp(String msgId) {
        return getDataAsLong(getMessageData(MessageData.KEY_TIMESTAMP_SENT, msgId));
    }

    @Override
    public long getMessageTimestamp(String msgId) {
        return getDataAsLong(getMessageData(MessageData.KEY_TIMESTAMP, msgId));
    }

    @Override
    public Status getMessageStatus(String msgId) {
        return Status.valueOf(getDataAsInt(getMessageData(MessageData.KEY_STATUS, msgId)));
    }

    @Override
    public ReasonCode getMessageReasonCode(String msgId) {
        return ReasonCode.valueOf(getDataAsInt(getMessageData(MessageData.KEY_REASON_CODE, msgId)));
    }

    @Override
    public String getMessageMimeType(String msgId) {
        return getDataAsString(getMessageData(MessageData.KEY_MIME_TYPE, msgId));
    }

    @Override
    // TODO: This function should be replaced to use getDataAsString(getMessageData))
    // as soon as that method handles exceptions correctly (i.e. doesn't throw exception
    // when no row is found).
    public String getMessageChatId(String msgId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(MessageData.CONTENT_URI, msgId), new String[] {
                        MessageData.KEY_CHAT_ID
                    }, null, null, null);
            /* TODO: Handle cursor when null. */
            if (cursor.moveToNext()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(MessageData.KEY_CHAT_ID));
            }
            return null;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public Cursor getChatMessageData(String msgId) {
        Cursor cursor = mLocalContentResolver.query(
                Uri.withAppendedPath(MessageData.CONTENT_URI, msgId), null, null, null, null);
        /* TODO: Handle cursor when null. */
        if (cursor.moveToFirst()) {
            return cursor;
        }

        throw new ServerApiPersistentStorageException(
                "No row returned while querying for chat message data with msgId : ".concat(msgId));
    }

    @Override
    public String getChatMessageContent(String msgId) {
        return getDataAsString(getMessageData(MessageData.KEY_CONTENT, msgId));
    }

    @Override
    public Cursor getQueuedOneToOneChatMessages(ContactId contact) {
        String[] selectionArgs = new String[] {
            contact.toString()
        };
        return mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                SELECTION_QUEUED_ONETOONE_CHAT_MESSAGES, selectionArgs, ORDER_BY_TIMESTAMP_ASC);
    }

    @Override
    public void dequeueChatMessage(ChatMessage message) {
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_STATUS, Status.SENDING.toInt());
        values.put(MessageData.KEY_REASON_CODE, ReasonCode.UNSPECIFIED.toInt());
        /* Reset the timestamp as this message was originally queued and is sent only now. */
        values.put(MessageData.KEY_TIMESTAMP, message.getTimestamp());
        values.put(MessageData.KEY_TIMESTAMP_SENT, message.getTimestampSent());
        mLocalContentResolver.update(
                Uri.withAppendedPath(MessageData.CONTENT_URI, message.getMessageId()), values,
                null, null);
    }

    @Override
    public Cursor getQueuedGroupChatMessages(String chatId) {
        String[] selectionArgs = new String[] {
            chatId
        };
        return mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                SELECTION_QUEUED_GROUP_CHAT_MESSAGES, selectionArgs, ORDER_BY_TIMESTAMP_ASC);
    }

    @Override
    public void setChatMessageTimestamp(String msgId, long timestamp, long timestampSent) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Set chat message timestamp msgId=").append(msgId)
                    .append(", timestamp=").append(timestamp).append(", timestampSent=")
                    .append(timestampSent).toString());
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_TIMESTAMP, timestamp);
        values.put(MessageData.KEY_TIMESTAMP_SENT, timestampSent);

        mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId), values,
                null, null);
    }

    @Override
    public Map<ContactId, GroupChatEvent.Status> getGroupChatEvents(String chatId) {
        String[] selectionArgs = new String[] {
            chatId
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI,
                    PROJECTION_GROUP_CHAT_EVENTS, SELECTION_GROUP_CHAT_EVENTS, selectionArgs,
                    ORDER_BY_TIMESTAMP_ASC);
            // TODO check null cursor CR037
            if (!cursor.moveToFirst()) {
                return null;
            }
            Map<ContactId, GroupChatEvent.Status> groupChatEvents = new HashMap<ContactId, GroupChatEvent.Status>();
            int columnIdxStatus = cursor.getColumnIndexOrThrow(MessageData.KEY_STATUS);
            int columnIdxContact = cursor.getColumnIndexOrThrow(MessageData.KEY_CONTACT);
            do {
                GroupChatEvent.Status status = GroupChatEvent.Status.valueOf(cursor
                        .getInt(columnIdxStatus));
                ContactId contact = ContactUtil.createContactIdFromTrustedData(cursor
                        .getString(columnIdxContact));
                groupChatEvents.put(contact, status);
            } while (cursor.moveToNext());
            return groupChatEvents;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public boolean isOneToOneChatMessage(String msgId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    MessageData.CONTENT_URI.buildUpon().appendPath(msgId).build(), new String[] {
                            MessageData.KEY_CONTACT, MessageData.KEY_CHAT_ID
                    }, null, null, null);
            /* TODO: Handle cursor when null. */
            if (!cursor.moveToNext()) {
                return false;
            }
            String contactId = cursor.getString(cursor
                    .getColumnIndexOrThrow(MessageData.KEY_CONTACT));
            String chatId = cursor.getString(cursor.getColumnIndexOrThrow(MessageData.KEY_CHAT_ID));
            return chatId.equals(contactId);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void setChatMessageStatusDelivered(String msgId, long timestampDelivered) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("setChatMessageStatusDelivered msgId=").append(msgId)
                    .append(", timestampDelivered=").append(timestampDelivered).toString());
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_STATUS, Status.DELIVERED.toInt());
        values.put(MessageData.KEY_REASON_CODE, ReasonCode.UNSPECIFIED.toInt());
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, timestampDelivered);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);

        if (mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, null, null) < 1) {
            sLogger.warn("There was no message with msgId '" + msgId + "' to set to delivered.");
        }
    }

    @Override
    public void setChatMessageStatusDisplayed(String msgId, long timestampDisplayed) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("setChatMessageStatusDisplayed msgId=").append(msgId)
                    .append(", timestampDisplayed=").append(timestampDisplayed).toString());
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_STATUS, Status.DISPLAYED.toInt());
        values.put(MessageData.KEY_REASON_CODE, ReasonCode.UNSPECIFIED.toInt());
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, timestampDisplayed);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);

        if (mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, null, null) < 1) {
            sLogger.warn("There was no message with msgId '" + msgId + "' to set to displayed.");
        }
    }

    @Override
    public void clearMessageDeliveryExpiration(List<String> msgIds) {
        String[] selectionArgs = new String[msgIds.size()];
        selectionArgs = msgIds.toArray(selectionArgs);
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        mLocalContentResolver.update(MessageData.CONTENT_URI, values,
                SELECTION_BY_MULTIPLE_MSG_IDS, selectionArgs);
    }

    @Override
    public void setChatMessageDeliveryExpired(String msgId) {
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 1);
        mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId), values,
                null, null);
    }

    @Override
    public Cursor getUndeliveredOneToOneChatMessages() {
        return mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                SELECTION_BY_UNDELIVERED_ONETOONE_CHAT_MESSAGES, null, ORDER_BY_TIMESTAMP_ASC);
    }

    @Override
    public boolean isChatMessageExpiredDelivery(String msgId) {
        return getDataAsBoolean(getMessageData(MessageData.KEY_EXPIRED_DELIVERY, msgId));
    }
}
