package com.kintmin.presentation.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateInstallerTest {

    private val ipaUrl = "https://github.com/kintmin/JellyTube/releases/download/mobile/v2.1.0/JellyTube_2.1.0(31)_release.ipa"

    @Test
    fun `브라우저는 받을 주소를 그대로 연다`() {
        assertEquals(ipaUrl, AppUpdateInstaller.Browser.installUri(ipaUrl))
    }

    @Test
    fun `SideStore 에는 받을 주소를 쿼리 값으로 인코딩해 넘긴다`() {
        assertEquals(
            "sidestore://install?url=https%3A%2F%2Fgithub.com%2Fkintmin%2FJellyTube%2Freleases%2Fdownload%2Fmobile%2Fv2.1.0%2FJellyTube_2.1.0%2831%29_release.ipa",
            AppUpdateInstaller.SideStore.installUri(ipaUrl),
        )
    }

    @Test
    fun `UTF-8 글자도 바이트 단위로 인코딩한다`() {
        assertEquals("sidestore://install?url=%ED%95%9C", AppUpdateInstaller.SideStore.installUri("한"))
    }
}
