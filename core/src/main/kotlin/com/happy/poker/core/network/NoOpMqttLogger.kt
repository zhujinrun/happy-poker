package com.happy.poker.core.network

import org.eclipse.paho.client.mqttv3.logging.Logger
import java.util.ResourceBundle

class NoOpMqttLogger : Logger {
    override fun initialise(
        messageCatalog: ResourceBundle?,
        loggerID: String?,
        resourceName: String?
    ) = Unit

    override fun setResourceName(resourceName: String?) = Unit

    override fun isLoggable(level: Int): Boolean = false

    override fun severe(sourceClass: String?, sourceMethod: String?, msg: String?) = Unit

    override fun severe(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?
    ) = Unit

    override fun severe(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?,
        thrown: Throwable?
    ) = Unit

    override fun warning(sourceClass: String?, sourceMethod: String?, msg: String?) = Unit

    override fun warning(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?
    ) = Unit

    override fun warning(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?,
        thrown: Throwable?
    ) = Unit

    override fun info(sourceClass: String?, sourceMethod: String?, msg: String?) = Unit

    override fun info(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?
    ) = Unit

    override fun info(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?,
        thrown: Throwable?
    ) = Unit

    override fun config(sourceClass: String?, sourceMethod: String?, msg: String?) = Unit

    override fun config(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?
    ) = Unit

    override fun config(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?,
        thrown: Throwable?
    ) = Unit

    override fun fine(sourceClass: String?, sourceMethod: String?, msg: String?) = Unit

    override fun fine(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?
    ) = Unit

    override fun fine(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?,
        thrown: Throwable?
    ) = Unit

    override fun finer(sourceClass: String?, sourceMethod: String?, msg: String?) = Unit

    override fun finer(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?
    ) = Unit

    override fun finer(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?,
        thrown: Throwable?
    ) = Unit

    override fun finest(sourceClass: String?, sourceMethod: String?, msg: String?) = Unit

    override fun finest(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?
    ) = Unit

    override fun finest(
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?,
        thrown: Throwable?
    ) = Unit

    override fun log(
        level: Int,
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?,
        thrown: Throwable?
    ) = Unit

    override fun trace(
        level: Int,
        sourceClass: String?,
        sourceMethod: String?,
        msg: String?,
        inserts: Array<Any>?,
        thrown: Throwable?
    ) = Unit

    override fun formatMessage(msg: String?, inserts: Array<Any>?): String = msg.orEmpty()

    override fun dumpTrace() = Unit
}
