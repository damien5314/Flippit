package com.ddiehl.android.reversi

import com.ddiehl.android.logging.ConsoleLogger
import com.ddiehl.android.logging.ConsoleLoggingTree
import org.junit.After
import org.junit.Before
import timber.log.Timber

abstract class BaseTest {

    @Before
    fun setUp() {
        Timber.plant(ConsoleLoggingTree(ConsoleLogger()))
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }
}