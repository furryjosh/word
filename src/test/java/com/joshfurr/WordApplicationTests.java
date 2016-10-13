package com.joshfurr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class WordApplicationTests {

	//TODO: you will have to change the testfolder located on your machine in the project
	private static final String folderPath = "/Users/joshfurr/Renticity/word/src/main/resources/testfolder/";

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	private Gson gson = new Gson();

	private ResultActions resultActions;

	private ObjectMapper objectMapper = new ObjectMapper();

	private MediaType contentType = new MediaType( MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(),
			Charset.forName( "utf8" ) );

	@Before
	public void setUp(){
		mockMvc = MockMvcBuilders.webAppContextSetup( wac ).build();

		File f = new File( folderPath);
		if (f.exists() && f.isDirectory()) {
			deleteTestDir(f);
		}
	}

	@Test
	@Ignore //TODO: you will have to comment this out in order to run the tests
	public void findFileOnPath() throws Exception {

		Map<String, String> requestMap = new HashMap<>();

		requestMap.put("1", folderPath);

		Map<String, Map<String, Integer>> responseMap;

		resultActions = mockMvc.perform( post("/path/words")

				.accept(MediaType.APPLICATION_JSON)

				.content(this.gson.toJson(requestMap))

				.contentType(contentType) )

				.andDo(print());

		responseMap = objectMapper.readValue(resultActions.andReturn()

				.getResponse().getContentAsByteArray(), Map.class);

		boolean file = ( responseMap.get("1").get("file") == 2 );

		assertTrue("Test failed - file : " + responseMap.get("1").get("file"), file );

		boolean second = ( responseMap.get("1").get("second") == 1 );

		assertTrue("Test failed - second : " + responseMap.get("1").get("second"), second);
	}

	private static void deleteTestDir(File dir){

		File file = new File(dir.getAbsolutePath() + "/test/test/folder1/first.txt");
		file.delete();

		file = new File(dir.getAbsolutePath() + "/test/test/folder1");
		file.delete();

		file = new File(dir.getAbsolutePath() + "/test/test/second.txt");
		file.delete();

		file = new File(dir.getAbsolutePath() + "/test/test");
		file.delete();

		file = new File(dir.getAbsolutePath() + "/test");
		file.delete();

		file = new File(dir.getAbsolutePath());
		file.delete();
	}

}
