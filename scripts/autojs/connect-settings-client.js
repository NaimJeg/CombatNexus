// eval_js side=client; companion server uses -PverificationGamePort=25591.
(function () {
    var Minecraft = Java.loadClass("net.minecraft.client.Minecraft");
    var Screen = Java.loadClass("net.minecraft.client.gui.screens.TitleScreen");
    var Address = Java.loadClass("net.minecraft.client.multiplayer.resolver.ServerAddress");
    var Data = Java.loadClass("net.minecraft.client.multiplayer.ServerData");
    Java.loadClass("net.minecraft.client.gui.screens.ConnectScreen").startConnecting(
        new Screen(), Minecraft.getInstance(), Address.parseString("127.0.0.1:25591"),
        new Data("Settings verification", "127.0.0.1:25591", Data.Type.OTHER), false, null);
    return JSON.stringify({connecting:true});
})()
