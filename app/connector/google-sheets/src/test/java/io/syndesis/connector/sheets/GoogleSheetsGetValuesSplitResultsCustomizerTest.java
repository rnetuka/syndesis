/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.syndesis.connector.sheets;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.component.google.sheets.internal.GoogleSheetsApiCollection;
import org.apache.camel.component.google.sheets.internal.SheetsSpreadsheetsValuesApiMethod;
import org.apache.camel.component.google.sheets.stream.GoogleSheetsStreamConstants;
import org.apache.camel.support.DefaultExchange;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import io.syndesis.connector.sheets.model.RangeCoordinate;
import io.syndesis.connector.support.util.ConnectorOptions;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

@RunWith(Parameterized.class)
public class GoogleSheetsGetValuesSplitResultsCustomizerTest extends AbstractGoogleSheetsCustomizerTestSupport {

    private GoogleSheetsGetValuesCustomizer customizer;

    private final String range;
    private final String sheetName;
    private final String majorDimension;
    private final List<Object> values;
    private final String expectedValueModel;

    public GoogleSheetsGetValuesSplitResultsCustomizerTest(String range, String sheetName, String majorDimension, List<Object> values, String expectedValueModel) {
        this.range = range;
        this.sheetName = sheetName;
        this.majorDimension = majorDimension;
        this.values = values;
        this.expectedValueModel = expectedValueModel;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "A1", "Sheet1", RangeCoordinate.DIMENSION_ROWS, Collections.singletonList("a1"),
                        "{\"spreadsheetId\":\"%s\", \"A\":\"a1\"}"},
                { "A1:A5", "Sheet1", RangeCoordinate.DIMENSION_COLUMNS, Arrays.asList("a1", "a2", "a3", "a4", "a5"),
                        "{\"spreadsheetId\":\"%s\", \"#1\":\"a1\",\"#2\":\"a2\",\"#3\":\"a3\",\"#4\":\"a4\",\"#5\":\"a5\"}"},
                { "A1:B2", "Sheet1", RangeCoordinate.DIMENSION_ROWS, Arrays.asList("a1", "b1"),
                        "{\"spreadsheetId\":\"%s\", \"A\":\"a1\",\"B\":\"b1\"}"},
                { "A1:B2", "Sheet1", RangeCoordinate.DIMENSION_COLUMNS, Arrays.asList("a1", "a2"),
                        "{\"spreadsheetId\":\"%s\", \"#1\":\"a1\",\"#2\":\"a2\"}"}
        });
    }

    @Before
    public void setupCustomizer() {
        customizer = new GoogleSheetsGetValuesCustomizer();
    }

    @Test
    public void testBeforeConsumer() throws Exception {
        Map<String, Object> options = new HashMap<>();
        options.put("spreadsheetId", getSpreadsheetId());
        options.put("range", range);
        options.put("splitResults", true);

        customizer.customize(getComponent(), options);

        Exchange inbound = new DefaultExchange(createCamelContext());

        inbound.getIn().setHeader(GoogleSheetsStreamConstants.RANGE, sheetName + "!" + range);
        inbound.getIn().setHeader(GoogleSheetsStreamConstants.MAJOR_DIMENSION, majorDimension);
        inbound.getIn().setHeader(GoogleSheetsStreamConstants.RANGE_INDEX, 1);
        inbound.getIn().setHeader(GoogleSheetsStreamConstants.VALUE_INDEX, 1);

        inbound.getIn().setBody(values);
        getComponent().getBeforeConsumer().process(inbound);

        Assert.assertEquals(GoogleSheetsApiCollection.getCollection().getApiName(SheetsSpreadsheetsValuesApiMethod.class).getName(), ConnectorOptions.extractOption(options, "apiName"));
        Assert.assertEquals("get", ConnectorOptions.extractOption(options, "methodName"));

        String model = inbound.getIn().getBody(String.class);
        assertThatJson(model).isEqualTo(String.format(expectedValueModel, getSpreadsheetId()));
    }
}
