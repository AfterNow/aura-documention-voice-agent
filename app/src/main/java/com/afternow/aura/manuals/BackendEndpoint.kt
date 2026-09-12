package com.afternow.aura.manuals

/** USB remains independent of the saved LAN address. */
object BackendEndpoint {
    fun url(useLan:Boolean,host:String,port:String):String {
        if(!useLan)return "ws://127.0.0.1:8787"
        val parts=host.trim().split('.')
        require(parts.size==4&&parts.all{it.isNotEmpty()&&it.length<=3&&it.all{c->c in '0'..'9'}&&it.toInt() in 0..255}){
            "Enter the PC's IPv4 address, for example 192.168.1.42."
        }
        val numbers=parts.map{it.toInt()}
        require(numbers[0] in 1..223&&numbers[0]!=127){"Enter the PC's network address, not a loopback or broadcast address."}
        val value=port.trim().toIntOrNull()
        require(value!=null&&value in 1..65535){"Enter a port from 1 to 65535."}
        return "ws://${numbers.joinToString(".")}:$value"
    }
}
