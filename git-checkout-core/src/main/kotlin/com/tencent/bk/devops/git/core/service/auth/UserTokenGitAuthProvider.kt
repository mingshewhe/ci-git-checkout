/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.devops.git.core.service.auth

import com.tencent.bk.devops.git.core.api.IDevopsApi
import com.tencent.bk.devops.git.core.constant.ContextConstants
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.exception.ApiException
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.i18n.GitErrorsText
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.PlaceholderResolver.Companion.defaultResolver

/**
 * 根据userId获取oauth2 token
 */
class UserTokenGitAuthProvider(
    private val userId: String?,
    private val devopsApi: IDevopsApi,
    private val scmType: ScmType
) : IGitAuthProvider {

    override fun getAuthInfo(): AuthInfo {
        if (userId.isNullOrBlank()) {
            throw ParamInvalidException(errorMsg = "授权用户ID不能为空")
        }
        val token = if (scmType == ScmType.GITHUB) {
            getGithubOauthToken(userId)
        } else {
            getGitOauthToken(userId)
        }
        EnvHelper.putContext(ContextConstants.CONTEXT_USER_ID, userId)
        return OauthGitAuthProvider(token = token, userId = userId).getAuthInfo()
    }

    private fun getGitOauthToken(userId: String): String {
        val result = devopsApi.getOauthToken(userId = userId)
        if (result.isNotOk() || result.data == null) {
            throw ApiException(
                errorMsg =
                defaultResolver.resolveByMap(
                    content = GitErrorsText.get().emptyAccessToken ?: "access token is empty",
                    valueMap = EnvHelper.getContextMap()
                )
            )
        }
        return result.data!!.accessToken
    }

    private fun getGithubOauthToken(userId: String): String {
        val result = devopsApi.getGithubOauthToken(userId = userId)
        if (result.isNotOk() || result.data == null) {
            throw ApiException(
                errorMsg =
                defaultResolver.resolveByMap(
                    content = GitErrorsText.get().emptyAccessToken ?: "access token is empty",
                    valueMap = EnvHelper.getContextMap()
                )
            )
        }
        return result.data!!.accessToken
    }
}
