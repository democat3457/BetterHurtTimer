package arekkuusu.betterhurttimer;

import arekkuusu.betterhurttimer.api.BHTAPI;
import arekkuusu.betterhurttimer.api.capability.HealthCapability;
import arekkuusu.betterhurttimer.api.capability.HurtCapability;
import arekkuusu.betterhurttimer.api.capability.data.HurtSourceInfo;
import arekkuusu.betterhurttimer.client.ClientProxy;
import arekkuusu.betterhurttimer.common.ServerProxy;
import arekkuusu.betterhurttimer.common.command.CommandExport;
import arekkuusu.betterhurttimer.common.proxy.IProxy;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(BHT.MOD_ID)
public final class BHT {

    //Useful names
    public static final String MOD_ID = "betterhurttimer";
    public static final String MOD_NAME = "Better Hurt Timer";

    private static IProxy proxy;
    public static final Logger LOG = LogManager.getLogger(MOD_NAME);

    public static IProxy getProxy() {
        return proxy;
    }

    public BHT() {
        proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> ServerProxy::new);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BHTConfig.Holder.CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BHTConfig.Holder.COMMON_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupClient);
        MinecraftForge.EVENT_BUS.addListener(this::setupServer);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onFingerprintViolation);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onModConfigEvent);
    }

    public void setup(final FMLCommonSetupEvent event) {
        HurtCapability.init();
    }

    public void setupClient(final FMLClientSetupEvent event) {
        HealthCapability.init();
    }

    public void setupServer(final FMLServerStartingEvent event) {
        CommandExport.register(event.getServer().getCommandManager().getDispatcher());
    }

    public void onFingerprintViolation(final FMLFingerprintViolationEvent event) {
        LOG.warn("Invalid fingerprint detected!");
    }

    public void onModConfigEvent(final ModConfig.ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if (config.getSpec() == BHTConfig.Holder.CLIENT_SPEC) {
            BHTConfig.Setup.client(config);
            LOG.debug("Baked client config");
        } else if (config.getSpec() == BHTConfig.Holder.COMMON_SPEC) {
            BHTConfig.Setup.server(config);
            this.initAttackFrames();
            this.initDamageFrames();
            LOG.debug("Baked server config");
        }
    }

    public void initAttackFrames() {
        BHTAPI.ATTACK_THRESHOLD_MAP.clear();
        String patternAttackFrames = "^(.*:.*):((\\d*\\.)?\\d+)$";
        Pattern r = Pattern.compile(patternAttackFrames);
        for (String s : BHTConfig.Runtime.AttackFrames.attackThreshold) {
            Matcher m = r.matcher(s);
            if (m.matches()) {
                BHTAPI.addAttacker(new ResourceLocation(m.group(1)), Double.parseDouble(m.group(2)));
            } else {
                BHT.LOG.warn("[Attack Frames Config] - String " + s + " is not a valid format");
            }
        }
    }

    public void initDamageFrames() {
        BHTAPI.DAMAGE_SOURCE_INFO_MAP.clear();
        String patternAttackFrames = "^(.*):(true|false):?(\\d*)";
        Pattern r = Pattern.compile(patternAttackFrames);
        for (String s : BHTConfig.Runtime.DamageFrames.damageSource) {
            Matcher m = r.matcher(s);
            if (m.matches()) {
                BHTAPI.addSource(new HurtSourceInfo(m.group(1), Boolean.parseBoolean(m.group(2)), Integer.parseInt(m.group(3))));
            } else {
                BHT.LOG.warn("[Damage Frames Config] - String " + s + " is not a valid format");
            }
        }
    }
}