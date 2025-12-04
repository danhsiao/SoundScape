package com.cs407.soundscape.data

import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    suspend fun insertUser(user: UserEntity): Long {
        return userDao.insert(user)
    }

    suspend fun getUserByCredentials(username: String, password: String): UserEntity? {
        return userDao.getUserByCredentials(username, password)
    }

    suspend fun getUserByUsername(username: String): UserEntity? {
        return userDao.getUserByUsername(username)
    }

    fun getUserById(userId: Int): Flow<UserEntity?> {
        return userDao.getUserById(userId)
    }

    suspend fun getUserByIdSync(userId: Int): UserEntity? {
        return userDao.getUserByIdSync(userId)
    }
}

