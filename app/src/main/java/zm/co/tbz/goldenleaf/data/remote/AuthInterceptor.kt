package zm.co.tbz.goldenleaf.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // TODO: Add auth token to request
        return chain.proceed(request)
    }
}
