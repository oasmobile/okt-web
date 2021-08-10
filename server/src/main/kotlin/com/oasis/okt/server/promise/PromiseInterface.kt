package com.oasis.okt.server.promise

@Suppress("unused", "UNUSED_PARAMETER")
abstract class PromiseInterface<ResultType>(waitFor: Waitable? = null) : Rejectable, Waitable(waitFor) {
    enum class PromiseState(value: String) {
        PENDING("pending"),
        FULFILLED("fulfilled"),
        REJECTED("rejected"),
    }

    var state: PromiseState = PromiseState.PENDING
    var result: ResultType? = null
    var error: Throwable? = null

    abstract fun <HandledResultType> then(
        run: Rejectable.(result: ResultType) -> HandledResultType
    ): PromiseInterface<HandledResultType>

    abstract fun otherwise(
        run: PromiseInterface<ResultType>.(reason: Throwable) -> Unit
    ): PromiseInterface<ResultType>

    inline fun <reified T> catches(
        crossinline onCaught: (e: T) -> Unit
    ): PromiseInterface<ResultType> {
        return this.otherwise {
            if (it is T) {
                onCaught(it)
            } else {
                throw it
            }
        }
    }

    fun takeDelegation(other: PromiseInterface<ResultType>): PromiseInterface<ResultType> {
        this.block(other)
        this.then { other.resolve(it) }
            .otherwise { other.reject(it) }

        return this
    }

    abstract fun eventually(
        run: () -> Unit
    ): PromiseInterface<ResultType>

    abstract fun resolve(result: ResultType)

    abstract override fun reject(reason: Throwable)

    abstract fun cancel()

    abstract fun unwrap(): ResultType
}