package net.mcreator.commandcookieselectronicmod.client;

import java.util.List;

/** Bundled BlockNet programs. IDs are stable across saves. */
public final class LaptopCatalog {
    public record App(int id,String key,String name,String category,String tagline,String instructions,int color) {}
    public static final List<App> ALL=List.of(
        new App(24,"chromium","Chromium","Tools","Browse real websites with Google search.","Windows 64-bit. The browser downloads its engine on first use. Type a URL or search; click links and scroll normally.",0xFF70A7E5),
        new App(8,"snake","Snake","Games","A classic, with a blocky bite.","Arrows or WASD steer. Eat apples, avoid walls and your tail. Space pauses.",0xFF8FDAA1),
        new App(9,"2048","2048","Games","Merge your way to 2048.","Arrow keys slide the tiles. Equal tiles merge once per move. Undo rescues one move.",0xFFE8BB7D),
        new App(10,"mines","Mines","Games","Find every safe square.","Click to reveal; right-click to flag. Your first reveal is safe. Numbers count nearby mines.",0xFF93BBCE),
        new App(11,"breakout","Breakout","Games","A tiny neon brick breaker.","Left/Right or A/D move the paddle. Space launches and pauses. Clear all the bricks.",0xFFEEA8B8),
        new App(12,"blockfall","Blockfall","Games","Seven shapes. Endless stacks.","Arrows move and rotate; Down soft-drops, Space hard-drops. P pauses. Clear full rows.",0xFFC3ACEC),
        new App(13,"memory","Memory","Games","Match the pixel symbols.","Reveal two cards. Match all pairs with as few turns as possible.",0xFF8FC9E9),
        new App(14,"tictactoe","Tic Tac Toe","Games","Challenge the laptop.","You are X. Make three in a row; the laptop plays O. Choose Casual or Expert.",0xFFEDA773),
        new App(15,"reaction","Reaction","Games","How quick are your fingers?","Press Start, wait for green, then click. Early clicks don't count. Your fastest time saves.",0xFFDBDB84),
        new App(16,"paint","Paint","Tools","A pocket pixel-art studio.","Draw on a 32 x 32 canvas. Pick a color, erase, undo, then export to your shared Gallery.",0xFFE9A4C8),
        new App(17,"planner","Planner","Tools","Keep your next build on track.","Add tasks, tick them off, or delete finished ones. Tasks save between visits.",0xFFA4D5BB),
        new App(18,"calendar","Calendar","Tools","Your days, at a glance.","Browse months and jump back to today. The calendar follows your computer's local date.",0xFFD6AE9D),
        new App(19,"converter","Converter","Tools","Useful units in one window.","Convert length, mass, temperature, time or stacks. Type a number, choose units, and swap.",0xFF8ED6DB),
        new App(20,"focus","Focus Timer","Tools","A little time to build.","Start a focus or break timer. It remembers the remaining time even after you leave the laptop.",0xFFC9B6E9),
        new App(21,"system","System","Tools","Know your corner of the world.","Live position, direction, biome, world time, FPS, and memory; copy your coordinates.",0xFFB2C7D2)
    );
    public static App find(int id){return ALL.stream().filter(a->a.id()==id).findFirst().orElse(null);}
    public static App find(String key){return ALL.stream().filter(a->a.key().equalsIgnoreCase(key)||a.name().equalsIgnoreCase(key)).findFirst().orElse(null);}
    private LaptopCatalog(){}
}
