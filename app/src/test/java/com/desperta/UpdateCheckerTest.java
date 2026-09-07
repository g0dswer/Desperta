package com.desperta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

/** Pure tests for release selection and update integrity helpers. */
public final class UpdateCheckerTest {
  @Test
  public void versionsOrderStableAfterPrereleaseAndIgnoreVPrefix() {
    assertTrue(UpdateChecker.Version.parse("v1.2.0").compareTo(UpdateChecker.Version.parse("1.1.9")) > 0);
    assertTrue(UpdateChecker.Version.parse("1.2.0").compareTo(UpdateChecker.Version.parse("1.2.0-rc.1")) > 0);
    assertTrue(UpdateChecker.Version.parse("1.2.0-beta.2").compareTo(UpdateChecker.Version.parse("1.2.0-beta.1")) > 0);
    assertTrue(UpdateChecker.Version.parse("1.2.0-alpha").compareTo(UpdateChecker.Version.parse("1.2.0-beta")) < 0);
    assertEquals("1.2.0-rc.1", UpdateChecker.Version.parse("v1.2.0-rc.1+build9").toString());
    assertTrue(UpdateChecker.Version.parse("1.2.0-alpha.10").compareTo(UpdateChecker.Version.parse("1.2.0-alpha.2")) > 0);
  }

  @Test
  public void selectsNewestPublishedApkAndFiltersDraftsAndPreviews() {
    String releases =
        "["
            + "{\"tag_name\":\"v1.4.0\",\"name\":\"Draft\",\"draft\":true,\"prerelease\":false,\"assets\":[{\"name\":\"Desperta-1.4.0.apk\",\"browser_download_url\":\"https://github.com/g0dswer/Desperta/releases/download/v1.4.0/Desperta-1.4.0.apk\"}]},"
            + "{\"tag_name\":\"v1.3.0-beta.1\",\"name\":\"Beta\",\"draft\":false,\"prerelease\":true,\"assets\":[{\"name\":\"Desperta-1.3.0-beta.1.apk\",\"browser_download_url\":\"https://github.com/g0dswer/Desperta/releases/download/v1.3.0-beta.1/Desperta-1.3.0-beta.1.apk\"}]},"
            + "{\"tag_name\":\"v1.2.0\",\"name\":\"Stable\",\"draft\":false,\"prerelease\":false,\"html_url\":\"https://github.com/g0dswer/Desperta/releases/tag/v1.2.0\",\"assets\":[{\"name\":\"Desperta-1.2.0.apk\",\"browser_download_url\":\"https://github.com/g0dswer/Desperta/releases/download/v1.2.0/Desperta-1.2.0.apk\"}]},"
            + "{\"tag_name\":\"v1.5.0\",\"name\":\"No APK\",\"draft\":false,\"prerelease\":false,\"assets\":[{\"name\":\"SHA256SUMS.txt\",\"browser_download_url\":\"https://github.com/g0dswer/Desperta/releases/download/v1.5.0/SHA256SUMS.txt\"}]}"
            + "]";
    UpdateChecker.Result stable = UpdateChecker.parseReleaseList(releases, "1.1.0", false);
    assertEquals(UpdateChecker.Result.Status.UPDATE_AVAILABLE, stable.status);
    assertNotNull(stable.release);
    assertEquals("1.2.0", stable.release.version);

    UpdateChecker.Result preview = UpdateChecker.parseReleaseList(releases, "1.1.0", true);
    assertEquals(UpdateChecker.Result.Status.UPDATE_AVAILABLE, preview.status);
    assertEquals("1.3.0-beta.1", preview.release.version);

    UpdateChecker.Result current = UpdateChecker.parseReleaseList(releases, "1.2.0", false);
    assertEquals(UpdateChecker.Result.Status.UP_TO_DATE, current.status);
  }

  @Test
  public void checksumParserAcceptsBinaryAndTextFileForms() {
    String hash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    assertEquals(hash, UpdateChecker.findChecksum(hash + "  Desperta-1.2.0.apk\n", "Desperta-1.2.0.apk"));
    assertEquals(hash, UpdateChecker.findChecksum(hash + " *Desperta-1.2.0.apk\n", "Desperta-1.2.0.apk"));
    assertEquals("", UpdateChecker.findChecksum(hash + "  outro.apk\n", "Desperta-1.2.0.apk"));
  }

  @Test
  public void malformedResponseAndReleasesWithoutSafeApkStayCurrent() {
    UpdateChecker.Result malformed = UpdateChecker.parseReleaseList("not-json", "1.2.0", true);
    assertEquals(UpdateChecker.Result.Status.ERROR, malformed.status);

    String unsafe =
        "["
            + "{\"tag_name\":\"v9.0.0\",\"draft\":true,\"assets\":[{\"name\":\"Desperta.apk\",\"browser_download_url\":\"https://github.com/g0dswer/Desperta/releases/download/v9.0.0/Desperta.apk\"}]},"
            + "{\"tag_name\":\"v8.0.0\",\"draft\":false,\"prerelease\":false,\"assets\":[{\"name\":\"SHA256SUMS.txt\",\"browser_download_url\":\"https://github.com/g0dswer/Desperta/releases/download/v8.0.0/SHA256SUMS.txt\"}]},"
            + "{\"tag_name\":\"v7.0.0\",\"draft\":false,\"prerelease\":false,\"assets\":[{\"name\":\"Desperta.apk\",\"browser_download_url\":\"https://example.com/Desperta.apk\"}]},"
            + "{\"name\":\"No tag\",\"draft\":false,\"prerelease\":false,\"assets\":[{\"name\":\"Desperta.apk\",\"browser_download_url\":\"https://github.com/g0dswer/Desperta/releases/download/unknown/Desperta.apk\"}]}"
            + "]";
    UpdateChecker.Result result = UpdateChecker.parseReleaseList(unsafe, "1.2.0", true);
    assertEquals(UpdateChecker.Result.Status.UP_TO_DATE, result.status);
  }

  @Test
  public void invalidDownloadOriginIsRejectedBeforeNetworkAccess() {
    UpdateChecker.ReleaseInfo release =
        new UpdateChecker.ReleaseInfo(
            "v9.0.0",
            "9.0.0",
            "unsafe",
            "https://example.com/release",
            "https://example.com/Desperta.apk",
            "Desperta.apk",
            "",
            "",
            "",
            false);
    try {
      UpdateChecker.downloadAndVerify(null, release, null);
    } catch (IOException expected) {
      assertTrue(expected.getMessage().contains("inválido"));
      return;
    }
    throw new AssertionError("download with a non-GitHub origin must fail closed");
  }
}
