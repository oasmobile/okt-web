package com.oasis.okt.server.promise

@Suppress("unused")
open class Promise<ResultType>(
    waitFor: Waitable? = null,
) : PromiseInterface<ResultType>(waitFor) {

    companion object {
        fun <T> resolvedPromise(result: T): Promise<T> {
            val p = Promise<T>()
            p.resolve(result)
            return p
        }

        fun <T> rejectedPromise(reason: Throwable): Promise<T> {
            val p = Promise<T>()
            p.reject(reason)
            return p
        }
    }

    private var _eventually: (() -> Unit) = {}
    private var eventually: (() -> Unit)
        get() = _eventually
        set(value) {
            val block = _eventually
            _eventually = {
                block.invoke()
                value.invoke()
            }
        }

    data class Handler<ResultType>(
        val onSuccess: (ResultType) -> Unit,
        val onFailure: (Throwable) -> Unit,
    )

    private var handlers: MutableList<Handler<ResultType>> = mutableListOf()

    override fun <HandledResultType> then(
        run: Rejectable.(ResultType) -> HandledResultType,
    ): PromiseInterface<HandledResultType> {
        val p = Promise<HandledResultType>(this)
        handlers.add(
            Handler(
                fun(result: ResultType) {
                    try {
                        val nextResult = p.run(result)
                        if (p.state == PromiseState.PENDING) {
                            try {
                                p.resolve(nextResult)
                            } catch (e: Throwable) {
                                // we will not reject p because p already has result
                            }
                        }
                    } catch (e: Throwable) {
                        p.reject(e)
                    }
                },
                fun(reason: Throwable) {
                    p.reject(reason)
                }
            )
        )
        handleResolution()
        return p
    }

    override fun otherwise(
        run: PromiseInterface<ResultType>.(reason: Throwable) -> Unit,
    ): PromiseInterface<ResultType> {
        val p = Promise<ResultType>(this)
        handlers.add(
            Handler(
                fun(result: ResultType) {
                    p.resolve(result)
                },
                fun(reason: Throwable) {
                    try {
                        p.run(reason)
                    } catch (e: Throwable) {
                        p.reject(e)
                    } finally {
                        if (p.state == PromiseState.PENDING) {
                            p.reject(reason)
                        }
                    }
                }
            )
        )
        handleResolution()
        return p
    }

    private fun handleResolution() {
        if (state == PromiseState.PENDING) {
            return
        }

        try {
            while (handlers.isNotEmpty()) {
                val handler = handlers[0]
                when {
                    state == PromiseState.FULFILLED && result != null -> {
                        handler.onSuccess(result!!)
                    }
                    state == PromiseState.REJECTED && error != null -> {
                        handler.onFailure(error!!)
                    }
                    else ->
                        throw IllegalStateException("Handling finished promise without a resolution!")
                }
                handlers.removeAt(0)
            }
        } finally {
            eventually()
            // reset block after invoked
            _eventually = {}
        }
    }

    override fun resolve(result: ResultType) {
        if (state != PromiseState.PENDING)
            throw IllegalStateException("Cannot resolve a promise whose state is already $state")

        state = PromiseState.FULFILLED
        this.result = result
        handleResolution()
    }

    override fun reject(reason: Throwable) {
        if (state != PromiseState.PENDING)
            throw IllegalStateException("Cannot reject a promise whose state is already $state")

        state = PromiseState.REJECTED
        this.error = reason
        handleResolution()
    }

    override fun cancel() {
        if (state != PromiseState.PENDING) {
            throw IllegalStateException("Cannot cancel a promise whose state is not pending!")
        }
    }

    override fun await() {
        if (state == PromiseState.PENDING) {
            super.await()
        }
    }

    override fun unwrap(): ResultType {
        await()
        when (state) {
            PromiseState.FULFILLED ->
                return result ?: throw RejectionException("result not available when promise is fulfilled")
            PromiseState.REJECTED ->
                if (error is Throwable) throw error as Throwable
                else throw RejectionException(error)
            else ->
                throw IllegalStateException("Cannot unwrap a promise whose state is not resolved!")
        }
    }

    class RejectionException(val reason: Any?) : Throwable("Promise failed", null)

    override fun eventually(run: () -> Unit): PromiseInterface<ResultType> {
        eventually = run
        handleResolution()
        return this
    }
}
