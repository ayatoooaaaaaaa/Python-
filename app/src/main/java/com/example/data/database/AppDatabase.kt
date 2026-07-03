package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BotConfigDao
import com.example.data.dao.ModuleDao
import com.example.data.dao.ScriptDao
import com.example.data.model.BotConfig
import com.example.data.model.ModulePackage
import com.example.data.model.Script
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Script::class, ModulePackage::class, BotConfig::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao
    abstract fun moduleDao(): ModuleDao
    abstract fun botConfigDao(): BotConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pybot_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            // 1. Populate default scripts
            val scriptDao = db.scriptDao()
            val welcomeBotId = scriptDao.insertScript(
                Script(
                    name = "welcome_bot.py",
                    description = "A friendly Discord bot that welcomes new members and responds to simple greeting commands.",
                    content = """# Welcome Bot script
import discord
from discord.ext import commands

bot = commands.Bot(command_prefix='!')

@bot.event
async def on_ready():
    print(f'Logged in as {bot.user}!')
    print('Bot is ready to welcome new users!')

@bot.event
async def on_member_join(member):
    channel = bot.get_channel(123456789) # Replace with your channel ID
    if channel:
        await channel.send(f'Welcome to the server, {member.mention}! 🎉')

@bot.command()
async def hello(ctx):
    ""\"Greets the user""\"
    await ctx.send(f'Hello {ctx.author.name}! Hope you have a great day!')

@bot.command()
async def info(ctx):
    ""\"Provides bot information""\"
    await ctx.send('I am a Discord bot running on PyBot Runner inside Android! 🤖📱')
"""
                )
            )

            scriptDao.insertScript(
                Script(
                    name = "ping_bot.py",
                    description = "A minimal ping-pong bot with command latency calculations.",
                    content = """# Simple Ping Bot
import discord
from discord.ext import commands

bot = commands.Bot(command_prefix='!')

@bot.event
async def on_ready():
    print(f'Ping Bot is online as {bot.user}!')

@bot.command()
async def ping(ctx):
    ""\"Responds with pong and latency""\"
    latency = round(bot.latency * 1000)
    await ctx.send(f'Pong! 🏓 ({latency}ms)')

@bot.command()
async def stats(ctx):
    ""\"Show basic bot stats""\"
    await ctx.send('Running smoothly on PyBot Android engine.')
"""
                )
            )

            // 2. Populate default bot configuration
            val botConfigDao = db.botConfigDao()
            botConfigDao.insertOrUpdate(
                BotConfig(
                    id = 1,
                    token = "", // Blank initially
                    selectedScriptId = welcomeBotId.toInt(),
                    status = "Offline"
                )
            )

            // 3. Populate default python packages
            val moduleDao = db.moduleDao()
            val modules = listOf(
                ModulePackage(
                    name = "discord.py",
                    version = "2.3.2",
                    description = "Modern, easy-to-use, feature-rich and async-ready API wrapper for Discord in Python.",
                    isInstalled = false,
                    size = "1.4 MB",
                    category = "Network"
                ),
                ModulePackage(
                    name = "requests",
                    version = "2.31.0",
                    description = "Elegant and simple HTTP library for Python, built for human beings.",
                    isInstalled = false,
                    size = "182 KB",
                    category = "Network"
                ),
                ModulePackage(
                    name = "beautifulsoup4",
                    version = "4.12.3",
                    description = "Screen-scraping library designed for quick turnaround projects like screen-scraping.",
                    isInstalled = false,
                    size = "115 KB",
                    category = "Scraping"
                ),
                ModulePackage(
                    name = "numpy",
                    version = "1.26.4",
                    description = "The fundamental package for scientific computing with Python.",
                    isInstalled = false,
                    size = "17.1 MB",
                    category = "Math"
                ),
                ModulePackage(
                    name = "pandas",
                    version = "2.2.1",
                    description = "Powerful data structures for data analysis, time series, and statistics.",
                    isInstalled = false,
                    size = "13.0 MB",
                    category = "Data"
                ),
                ModulePackage(
                    name = "pillow",
                    version = "10.2.0",
                    description = "The friendly PIL fork. Python Imaging Library adds image processing capabilities.",
                    isInstalled = false,
                    size = "3.2 MB",
                    category = "Utility"
                ),
                ModulePackage(
                    name = "aiohttp",
                    version = "3.9.3",
                    description = "Asynchronous HTTP client/server framework for asyncio and Python.",
                    isInstalled = false,
                    size = "1.1 MB",
                    category = "Network"
                ),
                ModulePackage(
                    name = "asyncio",
                    version = "3.4.3",
                    description = "Provides infrastructure for writing single-threaded concurrent code using coroutines.",
                    isInstalled = false,
                    size = "92 KB",
                    category = "Core"
                ),
                ModulePackage(
                    name = "colorama",
                    version = "0.4.6",
                    description = "Cross-platform colored terminal text in Python, making ANSI escape character sequences work.",
                    isInstalled = false,
                    size = "35 KB",
                    category = "Console"
                )
            )
            moduleDao.insertModules(modules)
        }
    }
}
