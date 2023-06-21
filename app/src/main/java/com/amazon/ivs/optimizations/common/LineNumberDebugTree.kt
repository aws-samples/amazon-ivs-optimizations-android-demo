package com.amazon.ivs.optimizations.common

import timber.log.Timber

private const val TIMBER_TAG = "IVS Best Practices"

class LineNumberDebugTree : Timber.DebugTree() {

    override fun createStackElementTag(element: StackTraceElement) =
        "$TIMBER_TAG: (${element.fileName}:${element.lineNumber}) #${element.methodName} "
}
