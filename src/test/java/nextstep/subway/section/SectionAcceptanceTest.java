package nextstep.subway.section;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static nextstep.subway.line.LineAcceptanceTest.*;

@DisplayName("지하철 구간 관련 기능")
public class SectionAcceptanceTest extends AcceptanceTest {

	private String upStationId;
	private String downStationId;
	private String lineUri;

	@BeforeEach
	public void setUp() {
		// 지하철_노선_생성
		Map<String, String> params = createLineParams();
		upStationId = params.get("upStationId");
		downStationId = params.get("downStationId");
		ExtractableResponse<Response> response = createNewLine(params);
		lineUri = response.header("Location");
	}

	@DisplayName("역 사이에 구간을 등록한다")
	@Test
	void addSection() {
		// given
		/*
		    [AS-IS]

			양재 --- 양재시민의숲
						 |
						 ↓
			양재 ------------------ 청계산입구

			[TO-BE]

			양재 --- 양재시민의숲 --- 청계산입구

		 */
		Map<String, String> params = new HashMap<>();
		params.put("upStationId", upStationId);
		params.put("downStationId", createStationId("양재시민의숲"));
		params.put("distance", "4");

		// when
		// 지하철_구간_등록_요청
		ExtractableResponse<Response> response = createNewSection(params);

		// then
		// 지하철_구간_등록됨
		assertResponseHttpStatusIsCreate(response);
	}

	@DisplayName("새로운 역을 상행 종점으로 구간을 등록한다")
	@Test
	void addSectionToUpEndPoint() {
		// given
		/*
		    [AS-IS]

			강남 --- 양재
			 |
			 ↓
			        양재 --- 청계산입구

			[TO-BE]

			강남 --- 양재 --- 청계산입구

		 */
		Map<String, String> params = new HashMap<>();
		params.put("upStationId", createStationId("강남"));
		params.put("downStationId", upStationId);
		params.put("distance", "4");

		// when
		// 지하철_구간_등록_요청
		ExtractableResponse<Response> response = createNewSection(params);

		// then
		// 지하철_구간_등록됨
		assertResponseHttpStatusIsCreate(response);
	}

	@DisplayName("새로운 역을 하행 종점으로 구간을 등록한다")
	@Test
	void addSectionToDownEndPoint() {
		// given
		/*
		    [AS-IS]

			         청계산입구 --- 판교(판교테크노밸리)
			 						|
			 						↓
			양재 --- 청계산입구

			[TO-BE]

			양재 --- 청계산입구 --- 판교(판교테크노밸리)

		 */
		Map<String, String> params = new HashMap<>();
		params.put("upStationId", downStationId);
		params.put("downStationId", createStationId("판교(판교테크노밸리)"));
		params.put("distance", "4");

		// when
		// 지하철_구간_등록_요청
		ExtractableResponse<Response> response = createNewSection(params);

		// then
		// 지하철_구간_등록됨
		assertResponseHttpStatusIsCreate(response);
	}

	@DisplayName("기존 역 사이 길이보다 크거나 같은 구간 등록")
	@Test
	void addSectionWithDistanceGreaterThanEqualExisting() {
		// given
		/*
			양재 --(15)-- 양재시민의숲
							 |
							 X
						 	 ↓
			양재 -----------(10)---------- 청계산입구
		 */
		Map<String, String> params = new HashMap<>();
		params.put("upStationId", upStationId);
		params.put("downStationId", createStationId("양재시민의숲"));
		params.put("distance", "15");

		// when
		// 지하철_구간_등록_요청
		ExtractableResponse<Response> response = createNewSection(params);

		// then
		// 지하철_구간_등록_실패됨
		assertResponseHttpStatusIsBadRequest(response);
	}

	@DisplayName("기존에 존재하는 구간의 상행역과 하행역으로 구간을 생성한다")
	@Test
	void addSectionWithDuplicationStation() {
		// given
		/*
			양재 --- 청계산입구
			 |			|
			 X			X
			 ↓		 	↓
			양재 --- 청계산입구
		*/
		Map<String, String> params = new HashMap<>();
		params.put("upStationId", upStationId);
		params.put("downStationId", downStationId);
		params.put("distance", "15");

		// when
		// 지하철_구간_등록_요청
		ExtractableResponse<Response> response = createNewSection(params);

		// then
		// 지하철_구간_등록_실패됨
		assertResponseHttpStatusIsBadRequest(response);
	}

	@DisplayName("상행역과 하행역 하나라도 포함되어 있지 않는 구간을 등록한다")
	@Test
	void addSectionNotEvenContainStations() {
		// given
		/*
			정자 --- 미금
			 |		 |
			 ?		 ?
			 ↓		 ↓
			양재 --- 청계산입구
		*/
		Map<String, String> params = new HashMap<>();
		params.put("upStationId", createStationId("정자"));
		params.put("downStationId", createStationId("미금"));
		params.put("distance", "15");

		// when
		// 지하철_구간_등록_요청
		ExtractableResponse<Response> response = createNewSection(params);

		// then
		// 지하철_구간_등록_실패됨
		assertResponseHttpStatusIsBadRequest(response);
	}

	private ExtractableResponse<Response> createNewSection(Map<String, String> params) {
		return RestAssured.given().log().all()
			.body(params)
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.when()
			.post(lineUri + "/section")
			.then().log().all()
			.extract();
	}
}