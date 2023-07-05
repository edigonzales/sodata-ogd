package ch.so.agi.ogd;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationTests {

    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;

	@Test
	void contextLoads() {
	}

    @Test
    public void index_Ok() throws Exception {
        assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/index.html", String.class))
                .contains("Offene Daten • Kanton Solothurn");
    }
    
    @Test
    public void search_Ok() throws Exception {
        assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/datasets?query=afu", String.class))
                .contains("Bewilligte");
    }
}
