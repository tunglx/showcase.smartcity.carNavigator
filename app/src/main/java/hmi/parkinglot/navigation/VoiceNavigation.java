package hmi.parkinglot.navigation;

import com.here.android.mpa.guidance.VoiceCatalog;
import com.here.android.mpa.guidance.VoicePackage;
import com.here.android.mpa.guidance.VoiceSkin;
import android.util.Log;

import java.util.List;

import hmi.parkinglot.Application;

/**
 * Created by jmcf on 6/11/15.
 */
public class VoiceNavigation {

    private static final String VOICE_LANG_CODE = "en-US";
    private static final String VOICE_LANG_CODE_ALT = "es-ES";
    private static final String VOICE_MARC_CODE = "eng";

    /**
     * Download the target voice skin
     */
    public static void downloadTargetVoiceSkin(final VoiceCatalog voiceCatalog) {
        VoiceSkin voiceSkin = null;

        // Get the list of voice skins from the voice catalog list
        List<VoiceSkin> localSkins = voiceCatalog.getLocalVoiceSkins();
        Log.e("voiceCatalog", localSkins == null ? "0" : localSkins.size()+"");

        // Search for an English TTS voice skin
        for (int i=0; i<localSkins.size(); i++) {
            VoiceSkin skin = localSkins.get(i);
            Log.e("voiceCatalog", skin.getLanguageCode());

            if (skin.getLanguageCode().compareToIgnoreCase(VOICE_LANG_CODE) == 0 ) {
                if (skin.getOutputType() == VoiceSkin.OutputType.TTS) {
                    voiceSkin = skin;
                }
            }
            else if (skin.getLanguageCode().compareToIgnoreCase(VOICE_LANG_CODE_ALT) == 0) {
                if (skin.getOutputType() == VoiceSkin.OutputType.TTS) {
                    voiceSkin = skin;
                }
            }
        }

        if(voiceSkin != null) {
            Application.mainActivity.setVoiceSkin(voiceSkin);
            return;
        }

        // Download catalog
        voiceCatalog.downloadCatalog(new VoiceCatalog.OnDownloadDoneListener() {
            @Override
            public void onDownloadDone(VoiceCatalog.Error error) {
                if (error == VoiceCatalog.Error.NONE) {
                    // Get the voice packages from the catalog
                    List<VoicePackage> voicePackages = voiceCatalog.getCatalogList();

                    // Search for an English TTS voice skin
                    for (final VoicePackage voicePackage : voicePackages) {
                        if (voicePackage.getMarcCode().equalsIgnoreCase(VOICE_MARC_CODE)
                                && voicePackage.isTts()) {

                            // Download voice skin
                            voiceCatalog.downloadVoice(voicePackage.getId(), new VoiceCatalog.OnDownloadDoneListener() {
                                        @Override
                                        public void onDownloadDone(VoiceCatalog.Error error) {
                                            if (error == VoiceCatalog.Error.NONE) {
                                                // Get the voice skin after it is downloaded
                                                VoiceSkin vs = voiceCatalog.getLocalVoiceSkin(voicePackage.getId());
                                                Application.mainActivity.setVoiceSkin(vs);

                                            } else {
                                                Log.e("Download voice package", String.format(
                                                        "Download voice package failed: %s", error.toString()));
                                            }
                                        }
                                    });

                            break;
                        }
                    }
                } else {
                    Log.e("Download voice catalog",
                            String.format("Download voice catalog failed: %s", error.toString()));
                }
            }
        });
    }
}
