package com.bishe;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;
class IllustrationCoverageTest {
 @Test void everyAttractionUsesAnExistingDistinctIllustration() throws Exception {
   var json=new ObjectMapper().readTree(getClass().getResourceAsStream("/graph-seed.json"));
   var images=new HashSet<String>();
   Path publicDir=Path.of(System.getProperty("user.dir")).getParent().resolve("frontend/public").toAbsolutePath().normalize();
   for(var attraction:json.path("attractions")) {
     String image=attraction.path("image").asText();
     assertTrue(image.startsWith("/images/"));
     assertTrue(images.add(image),"Each attraction needs a different illustration");
     Path file=publicDir.resolve(image.substring(1)).normalize();
     assertTrue(file.startsWith(publicDir)); assertTrue(Files.isRegularFile(file),"Illustration missing for "+attraction.path("name").asText());
   }
 }
}
