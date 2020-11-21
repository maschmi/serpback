package de.inw.serpent.serpback.type

class ErrorResult< T, out U> @PublishedApi internal constructor(val value: Any?, val isSuccess: Boolean)  {

    val isError: Boolean get() = !isSuccess
    inline fun <reified T> getOrNull():  T? = value as? T

    inline fun <reified U> errorOrNull(): U? = value as? U


    companion object {

        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("success")
        fun <T, U> success(value: T): ErrorResult<T,U> =
            ErrorResult(value, true)


        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("failure")
        fun <T, U> failure(value: U): ErrorResult<T,U> =
            ErrorResult(value, false)

    }
}

