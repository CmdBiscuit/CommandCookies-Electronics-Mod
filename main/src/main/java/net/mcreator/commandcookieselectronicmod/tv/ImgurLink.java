package net.mcreator.commandcookieselectronicmod.tv;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ImgurLink {
    private static final Pattern IMAGE = Pattern.compile("([A-Za-z0-9]{5,32})(?:\\.(png|jpe?g|gifv?))?", Pattern.CASE_INSENSITIVE);
    public static String normalize(String input) {
        if (input == null || input.isBlank()) return "";
        String value=input.strip();
        if(value.length()>512)throw new IllegalArgumentException("That link is too long.");
        if(!value.contains("://"))value="https://"+value;
        final URI uri;
        try { uri=URI.create(value); } catch(IllegalArgumentException ex) { throw new IllegalArgumentException("Paste a valid Imgur image link."); }
        String host=uri.getHost()==null?"":uri.getHost().toLowerCase(Locale.ROOT);
        if(!("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))
                ||!(host.equals("imgur.com")||host.equals("www.imgur.com")||host.equals("i.imgur.com"))
                ||uri.getRawUserInfo()!=null||uri.getPort()!=-1&&uri.getPort()!=443)
            throw new IllegalArgumentException("Use a link from imgur.com or i.imgur.com.");
        String path=uri.getRawPath();
        if(path==null)throw new IllegalArgumentException("Paste a single-image Imgur link.");
        if(path.startsWith("/a/")||path.startsWith("/gallery/"))throw new IllegalArgumentException("Use one image, rather than an Imgur album or gallery.");
        var match=IMAGE.matcher(path.replaceFirst("^/", ""));
        if(!match.matches())throw new IllegalArgumentException("Use a PNG, JPG or GIF image link.");
        String extension=match.group(2)==null?"png":match.group(2).toLowerCase(Locale.ROOT);
        if(extension.equals("gifv"))extension="gif";
        return "https://i.imgur.com/"+match.group(1)+"."+extension;
    }
    private ImgurLink() {}
}
