package com.lockr.app

import java.security.SecureRandom

data class PasswordGeneratorOptions(
    val length: Int = 20,
    val lowercase: Boolean = true,
    val uppercase: Boolean = true,
    val numbers: Boolean = true,
    val symbols: Boolean = true,
    val mixSelectedTypes: Boolean = true,
    val avoidLookalikes: Boolean = false
)

fun generateRandomPassword(options: PasswordGeneratorOptions, random: SecureRandom = SecureRandom()): String {
    require(options.length in 8..64) { "Length must be between 8 and 64" }
    val pools = buildList {
        if (options.lowercase) add(if (options.avoidLookalikes) "abcdefghjkmnpqrstuvwxyz" else "abcdefghijklmnopqrstuvwxyz")
        if (options.uppercase) add(if (options.avoidLookalikes) "ABCDEFGHJKMNPQRSTUVWXYZ" else "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        if (options.numbers) add(if (options.avoidLookalikes) "23456789" else "0123456789")
        if (options.symbols) add("!@#$%^&*()-_=+?")
    }
    require(pools.isNotEmpty()) { "Select at least one character type" }
    val all = pools.joinToString("")
    val result = mutableListOf<Char>()
    if (options.mixSelectedTypes) pools.forEach { pool -> result += pool[random.nextInt(pool.length)] }
    while (result.size < options.length) result += all[random.nextInt(all.length)]
    for (index in result.lastIndex downTo 1) {
        val other = random.nextInt(index + 1)
        val temp = result[index]
        result[index] = result[other]
        result[other] = temp
    }
    return result.joinToString("")
}
