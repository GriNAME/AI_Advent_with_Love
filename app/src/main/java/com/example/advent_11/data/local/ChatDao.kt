package com.example.advent_11.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query(
        """
        SELECT 
            t.id,
            t.title,
            t.model,
            t.createdAt,
            t.updatedAt,
            (
                SELECT m.content
                FROM chat_messages m
                WHERE m.chatId = t.id
                ORDER BY m.sequence DESC
                LIMIT 1
            ) AS lastMessage
        FROM chat_threads t
        ORDER BY t.updatedAt DESC
        """
    )
    fun observeChatThreads(): Flow<List<ChatThreadListItemLocal>>

    @Query("SELECT * FROM chat_threads WHERE id = :chatId LIMIT 1")
    fun observeChatThread(chatId: String): Flow<ChatThreadEntity?>

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY sequence ASC")
    fun observeMessages(chatId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatThread(thread: ChatThreadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Update
    suspend fun updateChatThread(thread: ChatThreadEntity)

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_threads WHERE id = :chatId LIMIT 1")
    suspend fun getChatThread(chatId: String): ChatThreadEntity?

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY sequence ASC")
    suspend fun getMessages(chatId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId AND sequence < :beforeSequence ORDER BY sequence ASC")
    suspend fun getMessagesBeforeSequence(chatId: String, beforeSequence: Int): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): ChatMessageEntity?

    @Query("SELECT MAX(sequence) FROM chat_messages WHERE chatId = :chatId")
    suspend fun getMaxSequence(chatId: String): Int?

    @Query("DELETE FROM chat_threads WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)
}
