/*
 *    This file is part of ReadonlyREST.
 *
 *    ReadonlyREST is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    ReadonlyREST is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with ReadonlyREST.  If not, see http://www.gnu.org/licenses/
 */
package tech.beshu.ror.core.requestcontext;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.junit.Test;
import tech.beshu.ror.AuditLogContext;
import tech.beshu.ror.commons.ResponseContext;
import tech.beshu.ror.commons.Verbosity;
import tech.beshu.ror.commons.shims.request.RequestContextShim;
import tech.beshu.ror.httpclient.HttpMethod;
import tech.beshu.ror.mocks.MockedESContext;
import tech.beshu.ror.requestcontext.AuditLogSerializer;
import tech.beshu.ror.requestcontext.SerializationTool;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SerializationToolTests {
  private static final Date THE_DATE = new Date(0);
  private static final Gson gson = new Gson();
  private static RequestContextShim requestContextShim = new RequestContextShim() {
    @Override
    public String getId() {
      return "123";
    }

    @Override
    public Set<String> getIndices() {
      return Sets.newHashSet("index1");
    }

    @Override
    public Date getTimestamp() {
      return THE_DATE;
    }

    @Override
    public String getAction() {
      return "theAction";
    }

    @Override
    public Map<String, String> getHeaders() {
      Map<String, String> m = new HashMap<>();
      m.put("h1", "v1");
      return m;
    }

    @Override
    public String getUri() {
      return "/_search";
    }

    @Override
    public String getHistoryString() {
      return "history";
    }

    @Override
    public Integer getContentLength() {
      return 0;
    }

    @Override
    public String getRemoteAddress() {
      return "OA";
    }

    @Override
    public String getType() {
      return "Object";
    }

    @Override
    public Long getTaskId() {
      return 0l;
    }

    @Override
    public String getMethodString() {
      return "GET";
    }

    @Override
    public Optional<String> getLoggedInUserName() {
      return Optional.of("user");
    }

    @Override
    public boolean involvesIndices() {
      return true;
    }
  };

  public static class MyCustomSerializer implements AuditLogSerializer {
      public MyCustomSerializer(){}
      @Override
      public Map<String, ?> createLoggableEntry(AuditLogContext context) {
        Map<String, Object > theMap = new HashMap<>(2);
        theMap.put("indices", context.getIndices());
        return theMap;
      }
  }

  @Test
  public void customSerializer(){

    SerializationTool st = new SerializationTool(new MockedESContext(MyCustomSerializer.class.getName()));

    String jString = st.toJson(new ResponseContext(ResponseContext.FinalState.ALLOWED, requestContextShim, null, Verbosity.INFO, "because", true));
    System.out.println(jString);
    assertEquals(jString,"{\"indices\":[\"index1\"]}");
  }

  @Test
  public void serializationTest() {

    SerializationTool st = new SerializationTool(new MockedESContext());
    String ser = st.toJson(new ResponseContext(ResponseContext.FinalState.ALLOWED, requestContextShim, null, Verbosity.INFO, "because", true));
    System.out.println(ser);
    Map<String, Object> ours = gson.fromJson("{\"errorMessage\":null,\"headers\":[\"h1\"],\"aclHistory\":\"history\",\"origin\":\"OA\",\"finalState\":\"ALLOWED\",\"taskId\":0,\"type\":\"Object\",\"reqMethod\":null,\"path\":\"/_search\",\"indices\":[\"index1\"],\"timestamp\":\"1970-01-01T00:00:00Z\",\"contentLenKb\":0,\"errorType\":null,\"processingMillis\":1510179210456,\"action\":\"theAction\",\"matchedBlock\":\"because\",\"id\":\"123\",\"contentLen\":0,\"user\":\"user\"}\n", Map.class);
    Map<String, Object> theirs = gson.fromJson(ser, Map.class);

    // The processing millis is calculated on the fly, cannot predict that
    assertTrue(((Double) theirs.get("processingMillis") > 0));
    theirs.put("processingMillis", ours.get("processingMillis"));

    assertEquals(ours, theirs);
  }
}
