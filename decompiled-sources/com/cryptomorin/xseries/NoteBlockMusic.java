/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Strings
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 *  org.bukkit.Instrument
 *  org.bukkit.Location
 *  org.bukkit.Note
 *  org.bukkit.Note$Tone
 *  org.bukkit.Sound
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 */
package com.cryptomorin.xseries;

import com.cryptomorin.xseries.XSound;
import com.google.common.base.Strings;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class NoteBlockMusic {
    private static final Map<String, Instrument> INSTRUMENTS = new HashMap<String, Instrument>(50);
    private static final Map<Instrument, XSound> INSTRUMENT_TO_SOUND = new EnumMap<Instrument, XSound>(Instrument.class);

    private NoteBlockMusic() {
    }

    @Nonnull
    public static XSound getSoundFromInstrument(@Nonnull Instrument instrument) {
        return INSTRUMENT_TO_SOUND.get(instrument);
    }

    @Nullable
    public static Note.Tone getNoteTone(char c) {
        switch (c) {
            case 'A': {
                return Note.Tone.A;
            }
            case 'B': {
                return Note.Tone.B;
            }
            case 'C': {
                return Note.Tone.C;
            }
            case 'D': {
                return Note.Tone.D;
            }
            case 'E': {
                return Note.Tone.E;
            }
            case 'F': {
                return Note.Tone.F;
            }
            case 'G': {
                return Note.Tone.G;
            }
        }
        return null;
    }

    public static CompletableFuture<Void> testMusic(@Nonnull Player player) {
        return NoteBlockMusic.playMusic(player, () -> ((Player)player).getLocation(), "PIANO,D,2,100 PIANO,B#1 200 PIANO,F 250 PIANO,E 250 PIANO,B 200 PIANO,A 100 PIANO,B 100 PIANO,E");
    }

    public static CompletableFuture<Void> fromFile(@Nonnull Player player, @Nonnull Supplier<Location> supplier, @Nonnull Path path) {
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader bufferedReader = Files.newBufferedReader(path, StandardCharsets.UTF_8);){
                String string;
                while ((string = bufferedReader.readLine()) != null) {
                    if ((string = string.trim()).isEmpty() || string.startsWith("#")) continue;
                    NoteBlockMusic.parseInstructions(string).play(player, supplier, true);
                }
            }
            catch (IOException iOException) {
                iOException.printStackTrace();
            }
        });
    }

    public static CompletableFuture<Void> playMusic(@Nonnull Player player, @Nonnull Supplier<Location> supplier, @Nullable String string) {
        return CompletableFuture.runAsync(() -> {
            if (Strings.isNullOrEmpty((String)string)) {
                return;
            }
            Sequence sequence = NoteBlockMusic.parseInstructions(string);
            sequence.play(player, supplier, true);
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    public static Sequence parseInstructions(@Nonnull CharSequence charSequence) {
        return new InstructionBuilder((CharSequence)charSequence).sequence;
    }

    private static void sleep(long l) {
        try {
            Thread.sleep(l);
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    @Nullable
    public static Note parseNote(@Nonnull String string) {
        Note.Tone tone = NoteBlockMusic.getNoteTone((char)(string.charAt(0) & 0x5F));
        if (tone == null) {
            return null;
        }
        int n = string.length();
        char c = ' ';
        int n2 = 0;
        if (n > 1) {
            char c2;
            c = string.charAt(1);
            if (NoteBlockMusic.isDigit(c)) {
                n2 = c - 48;
            } else if (n > 2 && NoteBlockMusic.isDigit(c2 = string.charAt(2))) {
                n2 = c2 - 48;
            }
            if (n2 < 0 || n2 > 2) {
                n2 = 0;
            }
        }
        return c == '#' ? Note.sharp((int)n2, (Note.Tone)tone) : (c == '_' ? Note.flat((int)n2, (Note.Tone)tone) : Note.natural((int)n2, (Note.Tone)tone));
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public static float noteToPitch(@Nonnull Note note) {
        return (float)Math.pow(2.0, ((double)note.getId() - 12.0) / 12.0);
    }

    @Nonnull
    public static BukkitTask playAscendingNote(@Nonnull Plugin plugin, final @Nonnull Player player, final @Nonnull Entity entity, final @Nonnull Instrument instrument, final int n, int n2) {
        Objects.requireNonNull(player, "Cannot play note from null player");
        Objects.requireNonNull(entity, "Cannot play note to null entity");
        if (n <= 0) {
            throw new IllegalArgumentException("Note ascend level cannot be lower than 1");
        }
        if (n > 7) {
            throw new IllegalArgumentException("Note ascend level cannot be greater than 7");
        }
        if (n2 <= 0) {
            throw new IllegalArgumentException("Delay ticks must be at least 1");
        }
        return new BukkitRunnable(){
            int repeating;
            {
                this.repeating = n;
            }

            public void run() {
                player.playNote(entity.getLocation(), instrument, Note.natural((int)1, (Note.Tone)Note.Tone.values()[n - this.repeating]));
                if (this.repeating-- == 0) {
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, (long)n2);
    }

    static {
        INSTRUMENT_TO_SOUND.put(Instrument.PIANO, XSound.BLOCK_NOTE_BLOCK_HARP);
        INSTRUMENT_TO_SOUND.put(Instrument.BASS_DRUM, XSound.BLOCK_NOTE_BLOCK_BASEDRUM);
        INSTRUMENT_TO_SOUND.put(Instrument.SNARE_DRUM, XSound.BLOCK_NOTE_BLOCK_SNARE);
        INSTRUMENT_TO_SOUND.put(Instrument.STICKS, XSound.BLOCK_NOTE_BLOCK_HAT);
        INSTRUMENT_TO_SOUND.put(Instrument.BASS_GUITAR, XSound.BLOCK_NOTE_BLOCK_BASS);
        INSTRUMENT_TO_SOUND.put(Instrument.FLUTE, XSound.BLOCK_NOTE_BLOCK_FLUTE);
        INSTRUMENT_TO_SOUND.put(Instrument.BELL, XSound.BLOCK_NOTE_BLOCK_BELL);
        INSTRUMENT_TO_SOUND.put(Instrument.GUITAR, XSound.BLOCK_NOTE_BLOCK_GUITAR);
        INSTRUMENT_TO_SOUND.put(Instrument.CHIME, XSound.BLOCK_NOTE_BLOCK_CHIME);
        INSTRUMENT_TO_SOUND.put(Instrument.XYLOPHONE, XSound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        INSTRUMENT_TO_SOUND.put(Instrument.IRON_XYLOPHONE, XSound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE);
        INSTRUMENT_TO_SOUND.put(Instrument.COW_BELL, XSound.BLOCK_NOTE_BLOCK_COW_BELL);
        INSTRUMENT_TO_SOUND.put(Instrument.DIDGERIDOO, XSound.BLOCK_NOTE_BLOCK_DIDGERIDOO);
        INSTRUMENT_TO_SOUND.put(Instrument.BIT, XSound.BLOCK_NOTE_BLOCK_BIT);
        INSTRUMENT_TO_SOUND.put(Instrument.BANJO, XSound.BLOCK_NOTE_BLOCK_BANJO);
        INSTRUMENT_TO_SOUND.put(Instrument.PLING, XSound.BLOCK_NOTE_BLOCK_PLING);
        INSTRUMENTS.put("HARP", Instrument.PIANO);
        INSTRUMENTS.put("BASEDRUM", Instrument.BASS_DRUM);
        INSTRUMENTS.put("BASE_DRUM", Instrument.BASS_DRUM);
        INSTRUMENTS.put("SNARE", Instrument.SNARE_DRUM);
        INSTRUMENTS.put("BASS", Instrument.BASS_GUITAR);
        INSTRUMENTS.put("COWBELL", Instrument.COW_BELL);
        block0: for (Instrument instrument : Instrument.values()) {
            String string = instrument.name();
            INSTRUMENTS.put(string, instrument);
            StringBuilder stringBuilder = new StringBuilder(String.valueOf(string.charAt(0)));
            int n = string.indexOf(95);
            if (n != -1) {
                stringBuilder.append(string.charAt(n + 1));
            }
            if (INSTRUMENTS.putIfAbsent(stringBuilder.toString(), instrument) == null) continue;
            for (int i = 0; i < string.length(); ++i) {
                char c = string.charAt(i);
                if (c == '_') {
                    ++i;
                    continue;
                }
                stringBuilder.append(c);
                if (INSTRUMENTS.putIfAbsent(stringBuilder.toString(), instrument) == null) continue block0;
            }
        }
    }

    private static final class InstructionBuilder {
        @Nonnull
        final CharSequence script;
        final int len;
        final StringBuilder instrumentBuilder = new StringBuilder(10);
        final StringBuilder pitchBuiler = new StringBuilder(3);
        final StringBuilder volumeBuilder = new StringBuilder(3);
        final StringBuilder restatementBuilder = new StringBuilder(10);
        final StringBuilder restatementDelayBuilder = new StringBuilder(10);
        final StringBuilder fermataBuilder = new StringBuilder(10);
        int i;
        boolean isSequence;
        boolean isBuilding;
        Sequence sequence = new Sequence();
        InstructionParserPhase phase = InstructionParserPhase.NEUTRAL;
        StringBuilder currentBuilder;

        public InstructionBuilder(@Nonnull CharSequence charSequence) {
            this.script = charSequence;
            this.len = charSequence.length();
            while (this.i < this.len) {
                char c = charSequence.charAt(this.i);
                switch (c) {
                    case '(': {
                        Sequence sequence = new Sequence();
                        sequence.parent = this.sequence;
                        this.sequence = sequence;
                        break;
                    }
                    case ')': {
                        if (this.sequence.parent == null) {
                            this.err("Cannot find start of the sequence for sequence at: " + this.i);
                        }
                        this.buildAndAddInstruction();
                        this.sequence = this.sequence.parent;
                        this.prepareHandlers();
                        this.phase = InstructionParserPhase.END_SEQ;
                        this.isSequence = true;
                        break;
                    }
                    case ' ': {
                        if (!this.isBuilding) break;
                        this.isBuilding = false;
                        switch (this.phase.ordinal()) {
                            case 6: {
                                this.buildAndAddInstruction();
                                this.prepareHandlers();
                                break;
                            }
                            case 2: 
                            case 5: {
                                this.phase = InstructionParserPhase.FERMATA;
                                this.currentBuilder = this.fermataBuilder;
                            }
                        }
                        break;
                    }
                    case ':': {
                        if (this.phase == InstructionParserPhase.NOTE) {
                            this.currentBuilder = this.volumeBuilder;
                            break;
                        }
                        this.err("Unexpected ':' pitch-volume separator at " + this.i + " with current phase: " + (Object)((Object)this.phase));
                        break;
                    }
                    case ',': {
                        switch (this.phase.ordinal()) {
                            case 1: {
                                this.currentBuilder = this.pitchBuiler;
                                break;
                            }
                            case 2: 
                            case 3: {
                                this.currentBuilder = this.restatementBuilder;
                                break;
                            }
                            case 4: {
                                this.currentBuilder = this.restatementDelayBuilder;
                                break;
                            }
                            default: {
                                this.err("Unexpected phase '" + (Object)((Object)this.phase) + "' at index: " + this.i);
                            }
                        }
                        this.isBuilding = false;
                        this.phase = this.phase.next();
                        break;
                    }
                    default: {
                        if (this.phase == InstructionParserPhase.NEUTRAL || this.canBuildInstructionInPhase() && InstructionParserPhase.INSTRUMENT.checkup(c) != '\u0000') {
                            this.currentBuilder = this.instrumentBuilder;
                            if (this.phase == InstructionParserPhase.FERMATA) {
                                this.buildAndAddInstruction();
                                this.prepareHandlers();
                            }
                            this.phase = InstructionParserPhase.INSTRUMENT;
                        }
                        this.isBuilding = true;
                        if ((c = this.phase.checkup(c)) == '\u0000') {
                            this.err("Unexpected char at index " + this.i + " with phase " + (Object)((Object)this.phase) + ": " + charSequence.charAt(this.i));
                        }
                        this.currentBuilder.append(c);
                    }
                }
                ++this.i;
            }
            this.buildAndAddInstruction();
            this.sequence = this.getRoot();
        }

        private Instruction buildInstruction() {
            Instruction instruction;
            int n;
            int n2 = this.fermataBuilder.length() == 0 ? 0 : Integer.parseInt(this.fermataBuilder.toString());
            int n3 = this.restatementBuilder.length() == 0 ? 1 : Integer.parseInt(this.restatementBuilder.toString());
            int n4 = n = this.restatementDelayBuilder.length() == 0 ? 0 : Integer.parseInt(this.restatementDelayBuilder.toString());
            if (this.isSequence) {
                instruction = new Sequence(n3, n, n2);
            } else {
                String string = this.instrumentBuilder.toString();
                Instrument instrument = (Instrument)INSTRUMENTS.get(string);
                XSound xSound = instrument == null ? (XSound)XSound.matchXSound(string).orElse(null) : NoteBlockMusic.getSoundFromInstrument(instrument);
                String string2 = this.pitchBuiler.toString();
                Note note = NoteBlockMusic.parseNote(string2);
                float f = note == null ? Float.parseFloat(string2) : NoteBlockMusic.noteToPitch(note);
                float f2 = 5.0f;
                if (this.volumeBuilder.length() != 0) {
                    f2 = Float.parseFloat(this.volumeBuilder.toString());
                }
                instruction = new Sound(xSound, f, f2, n3, n, n2);
            }
            return instruction;
        }

        private void prepareHandlers() {
            this.instrumentBuilder.setLength(0);
            this.pitchBuiler.setLength(0);
            this.volumeBuilder.setLength(0);
            this.restatementBuilder.setLength(0);
            this.restatementDelayBuilder.setLength(0);
            this.fermataBuilder.setLength(0);
            this.phase = InstructionParserPhase.NEUTRAL;
            this.isBuilding = false;
            this.isSequence = false;
        }

        private boolean canBuildInstructionInPhase() {
            switch (this.phase.ordinal()) {
                case 4: 
                case 5: 
                case 6: {
                    return true;
                }
            }
            return false;
        }

        private void buildAndAddInstruction() {
            this.sequence.addInstruction(this.buildInstruction());
        }

        private Sequence getRoot() {
            Sequence sequence = this.sequence;
            while (sequence.parent != null) {
                sequence = sequence.parent;
            }
            return sequence;
        }

        private String illustrateError() {
            return '\n' + this.script.toString() + '\n' + Strings.repeat((String)" ", (int)this.i) + '^';
        }

        private void err(String string) {
            throw new IllegalStateException(string + this.illustrateError());
        }
    }

    public static class Sequence
    extends Instruction {
        public Collection<Instruction> instructions = new ArrayList<Instruction>(16);

        public Sequence() {
            super(1, 0, 0);
        }

        public Sequence(Instruction instruction) {
            super(1, 0, 0);
            this.instructions.add(instruction);
        }

        public Sequence(int n, int n2, int n3) {
            super(n, n2, n3);
        }

        @Override
        public void play(Player player, Supplier<Location> supplier, boolean bl) {
            for (int i = this.restatement; i > 0; --i) {
                for (Instruction instruction : this.instructions) {
                    instruction.play(player, supplier, bl);
                }
                if (this.restatementFermata <= 0) continue;
                NoteBlockMusic.sleep(this.restatementFermata);
            }
            if (this.fermata > 0) {
                NoteBlockMusic.sleep(this.fermata);
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(200 + this.instructions.size() * 100);
            stringBuilder.append("Sequence:{restatement=").append(this.restatement).append(", restatementFermata=").append(this.restatementFermata).append(", fermata=").append(this.fermata).append(", instructions[");
            int n = 0;
            int n2 = this.instructions.size();
            for (Instruction instruction : this.instructions) {
                stringBuilder.append(instruction);
                if (++n >= n2) continue;
                stringBuilder.append(", ");
            }
            stringBuilder.append("]}");
            return stringBuilder.toString();
        }

        public void addInstruction(Instruction instruction) {
            instruction.parent = this;
            this.instructions.add(instruction);
        }

        @Override
        public long getEstimatedLength() {
            long l = (long)this.restatement * (long)this.restatementFermata;
            for (Instruction instruction : this.instructions) {
                l += instruction.getEstimatedLength();
            }
            return l;
        }
    }

    public static abstract class Instruction {
        @Nullable
        public Sequence parent;
        public int restatement;
        public int restatementFermata;
        public int fermata;

        public Instruction(int n, int n2, int n3) {
            this.restatement = n;
            this.restatementFermata = n2;
            this.fermata = n3;
        }

        public abstract void play(Player var1, Supplier<Location> var2, boolean var3);

        public long getEstimatedLength() {
            return (long)this.restatement * (long)this.restatementFermata;
        }
    }

    public static class Sound
    extends Instruction {
        public XSound sound;
        public float volume;
        public float pitch;

        public Sound(Instrument instrument, Note note, float f, int n, int n2, int n3) {
            super(n, n2, n3);
            this.sound = NoteBlockMusic.getSoundFromInstrument(instrument);
            this.pitch = NoteBlockMusic.noteToPitch(note);
            this.volume = f;
        }

        public Sound(XSound xSound, float f, float f2, int n, int n2, int n3) {
            super(n, n2, n3);
            this.sound = xSound;
            this.pitch = f;
            this.volume = f2;
        }

        public void setSound(Instrument instrument) {
            this.sound = NoteBlockMusic.getSoundFromInstrument(instrument);
        }

        public void setPitch(Note note) {
            this.pitch = NoteBlockMusic.noteToPitch(note);
        }

        @Override
        public void play(Player player, Supplier<Location> supplier, boolean bl) {
            org.bukkit.Sound sound = this.sound.parseSound();
            for (int i = this.restatement; i > 0; --i) {
                Location location = supplier.get();
                if (sound != null) {
                    if (bl) {
                        location.getWorld().playSound(location, sound, this.volume, this.pitch);
                    } else {
                        player.playSound(location, sound, this.volume, this.pitch);
                    }
                }
                if (this.restatementFermata <= 0) continue;
                NoteBlockMusic.sleep(this.restatementFermata);
            }
            if (this.fermata > 0) {
                NoteBlockMusic.sleep(this.fermata);
            }
        }

        public String toString() {
            return "Sound:{sound=" + (Object)((Object)this.sound) + ", pitch=" + this.pitch + ", volume=" + this.volume + ", restatement=" + this.restatement + ", restatementFermata=" + this.restatementFermata + ", fermata=" + this.fermata + '}';
        }
    }

    private static enum InstructionParserPhase {
        NEUTRAL{

            @Override
            protected InstructionParserPhase next() {
                return INSTRUMENT;
            }

            @Override
            protected char checkup(char c) {
                throw new AssertionError((Object)"Checkup should not be performed on NEUTRAL instruction parser phase");
            }
        }
        ,
        INSTRUMENT{

            @Override
            protected InstructionParserPhase next() {
                return NOTE;
            }

            @Override
            protected char checkup(char c) {
                if (c >= 'a' && c <= 'z') {
                    return (char)(c & 0x5F);
                }
                return c >= 'A' && c <= 'Z' || c == '_' || c == '-' ? c : (char)'\u0000';
            }
        }
        ,
        NOTE{

            @Override
            protected InstructionParserPhase next() {
                return RESTATEMENT;
            }

            @Override
            protected char checkup(char c) {
                if (c >= 'a' && c <= 'z') {
                    return (char)(c & 0x5F);
                }
                return c >= 'A' && c <= 'Z' || NoteBlockMusic.isDigit(c) || c == '.' || c == '_' || c == '#' ? c : (char)'\u0000';
            }
        }
        ,
        END_SEQ{

            @Override
            protected InstructionParserPhase next() {
                return RESTATEMENT;
            }

            @Override
            protected char checkup(char c) {
                return '\u0000';
            }
        }
        ,
        RESTATEMENT{

            @Override
            protected InstructionParserPhase next() {
                return RESTATEMENT_DELAY;
            }

            @Override
            protected char checkup(char c) {
                return NoteBlockMusic.isDigit(c) ? c : (char)'\u0000';
            }
        }
        ,
        RESTATEMENT_DELAY{

            @Override
            protected InstructionParserPhase next() {
                return FERMATA;
            }

            @Override
            protected char checkup(char c) {
                return NoteBlockMusic.isDigit(c) ? c : (char)'\u0000';
            }
        }
        ,
        FERMATA{

            @Override
            protected InstructionParserPhase next() {
                return NEUTRAL;
            }

            @Override
            protected char checkup(char c) {
                return NoteBlockMusic.isDigit(c) ? c : (char)'\u0000';
            }
        };


        protected abstract InstructionParserPhase next();

        protected abstract char checkup(char var1);
    }
}

