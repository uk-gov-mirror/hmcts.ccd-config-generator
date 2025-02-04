package uk.gov.hmcts.ccd.sdk;


import static org.assertj.core.api.Assertions.assertThat;


import java.io.File;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class IntegrationTest {
  @ClassRule
  public static TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void testMainMethod() {
    Main.main(new String[] {
        tmp.getRoot().getAbsolutePath(),
        "uk.gov.hmcts"
    });
    File expected = new File(new File(tmp.getRoot(), "production"), "CaseField.json");
    assertThat(expected).exists();
  }
}
