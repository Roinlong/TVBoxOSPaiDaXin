//package com.github.tvbox.osc.util;
//
//import android.util.Base64;
//
//import com.github.tvbox.osc.bean.Live;
//import com.github.tvbox.osc.bean.LiveChannelGroup;
//import com.github.tvbox.osc.bean.LiveChannelItem;
//
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class LiveParser {
//
//    private static final Pattern GROUP = Pattern.compile(".*group-title=\"(.?|.+?)\".*");
//    private static final Pattern LOGO = Pattern.compile(".*tvg-logo=\"(.?|.+?)\".*");
//    private static final Pattern NAME = Pattern.compile(".*,(.+?)$");
//
//    private static String extract(String line, Pattern pattern) {
//        Matcher matcher = pattern.matcher(line.trim());
//        if (matcher.matches()) return matcher.group(1);
//        return "";
//    }
//
//    public static void start(Live live) {
//        if (live.getGroups().size() > 0) return;
//        start(live, getText(live.getUrl()));
//    }
//
//    public static void start(Live live, String text) {
//        int number = 0;
//        if (live.getGroups().size() > 0) return;
//        if (text.trim().startsWith("#EXTM3U")) m3u(live, text); else txt(live, text);
//        for (Group group : live.getGroups()) {
//            for (Channel channel : group.getChannel()) {
//                channel.setNumber(++number);
//            }
//        }
//    }
//
//    private static void m3u(Live live, String text) {
//        LiveChannelItem channel = Channel.create("");
//        for (String line : text.split("\n")) {
//            if (line.startsWith("#EXTINF:")) {
//                LiveChannelGroup group = live.find(Group.create(extract(line, GROUP)));
//                channel = group.find(Channel.create(extract(line, NAME)));
//                channel.epg(live).setLogo(extract(line, LOGO));
//            } else if (line.contains("://")) {
//                channel.getUrls().add(line);
//            }
//        }
//    }
//
//    private static void txt(Live live, String text) {
//        for (String line : text.split("\n")) {
//            String[] split = line.split(",");
//            if (split.length < 2) continue;
//            if (line.contains("#genre#")) {
//                live.getGroups().add(Group.create(split[0]));
//            }
//            if (split[1].contains("://")) {
//                Group group = live.getGroups().get(live.getGroups().size() - 1);
//                group.find(Channel.create(split[0]).epg(live)).addUrls(split[1].split("#"));
//            }
//        }
//    }
//
//    private static String getText(String url) {
//        try {
//            if (url.startsWith("file")) return FileUtil.read(url);
//            else if (url.startsWith("http")) return OKHttp.newCall(url).execute().body().string();
//            else if (url.endsWith(".txt") || url.endsWith(".m3u")) return getText(Utils.convert(LiveConfig.getUrl(), url));
//            else if (url.length() > 0 && url.length() % 4 == 0) return getText(new String(Base64.decode(url, Base64.DEFAULT)));
//            else return "";
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "";
//        }
//    }
//}
