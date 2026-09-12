package com.afternow.aura.manuals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackendEndpointTest {
    @Test fun usbIgnoresLanFields(){
        assertEquals("ws://127.0.0.1:8787",BackendEndpoint.url(false,"","invalid"))
    }
    @Test fun localAddressAndCustomPort(){
        assertEquals("ws://192.168.30.42:8787",BackendEndpoint.url(true," 192.168.30.42 ","8787"))
        assertEquals("ws://10.0.0.3:9000",BackendEndpoint.url(true,"10.0.0.3","9000"))
    }
    @Test fun rejectsInvalidOrNonHostAddresses(){
        for(host in listOf("","ws://192.168.1.2","192.168.1.2/path","192.168.1.256","127.0.0.1","0.0.0.0","255.255.255.255","1.2.3","-1.2.3.4")){
            assertThrows(IllegalArgumentException::class.java){BackendEndpoint.url(true,host,"8787")}
        }
    }
    @Test fun rejectsInvalidPorts(){
        for(port in listOf("","0","65536","8787/path","NaN")){
            assertThrows(IllegalArgumentException::class.java){BackendEndpoint.url(true,"192.168.1.2",port)}
        }
    }
}
