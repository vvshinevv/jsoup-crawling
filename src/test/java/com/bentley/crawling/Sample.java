package com.bentley.crawling;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class Sample {

    @Test
    public void extractTradeSeaFoodForCountry() throws Exception {
        Document document = Jsoup.connect("http://www.trade-seafood.com/directory/seafood/country/index.htm").get();
        Elements tableElementList = document.select("table");
        Element tableElement = tableElementList.get(6);

        Elements trElementList = tableElement.select("tr");

        List<String> parentCategoryUrlList = new ArrayList<String>();
        for (int i = 1; i < trElementList.size(); i += 2) {
            Element trElement = trElementList.get(i);

            Elements aElementList = trElement.getElementsByTag("a");
            addToParentCategoryForCountry(parentCategoryUrlList, aElementList);
        }

        List<String> targetPageList = extractTargetPageList(parentCategoryUrlList);
        List<ExtractModel> extractModelList = extractForExcel(targetPageList);
        makeExcelFile(extractModelList);

    }

    @Test
    public void extractTradeSeaFoodForSpecies() throws Exception {
        Document document = Jsoup.connect("http://www.trade-seafood.com/directory/seafood/index.htm").get();
        Elements tableElementList = document.select("table");
        Element tableElement = tableElementList.get(5);

        Elements trElementList = tableElement.select("tr");

        List<String> parentCategoryUrlList = new ArrayList<String>();
        for (int i = 1; i < trElementList.size(); i += 2) {
            Element trElement = trElementList.get(i);

            Elements aElementList = trElement.getElementsByTag("a");
            addToParentCategoryForSpecies(parentCategoryUrlList, aElementList);
        }

        List<String> parentSubCategoryUrlList = new ArrayList<String>();
        for (String url : parentCategoryUrlList) {
            List<String> result = extractSubTradeSeaFoodForSpecies(url);
            parentSubCategoryUrlList.addAll(result);
        }

        List<String> targetPageList = extractTargetPageList(parentSubCategoryUrlList);
        List<ExtractModel> extractModelList = extractForExcel(targetPageList);
        makeExcelFile(extractModelList);
    }

    @Test
    public void extractTradeSeaFoodForImportExport() throws Exception {
        Document document = Jsoup.connect("http://www.trade-seafood.com/directory/seafood/importers/index.htm").get();
        Elements tableElementList = document.select("table");
        Element tableElement = tableElementList.get(5);

        Elements trElementList = tableElement.select("tr");
    }

    private List<String> extractSubTradeSeaFoodForSpecies(String url) throws Exception {
        Document document = Jsoup.connect(url).get();
        Elements tableElementList = document.select("table");
        Element tableElement = tableElementList.get(5);

        List<String> subUrl = new ArrayList<String>();
        Elements trElementList = tableElement.select("tr").select("a");
        getSeaFoodUrlHttp(subUrl, trElementList);

        return subUrl;
    }

    private void addToParentCategoryForSpecies(List<String> parentCategoryUrlList, Elements aElementList) {
        getSeaFoodUrlHttp(parentCategoryUrlList, aElementList);
    }

    private void getSeaFoodUrlHttp(List<String> parentCategoryUrlList, Elements aElementList) {
        for (Element element : aElementList) {
            if (element.attr("href").startsWith("http")) {
                parentCategoryUrlList.add(element.attr("href"));
            } else {
                String attributeHref = "http://www.trade-seafood.com/directory/seafood/" + element.attr("href");
                parentCategoryUrlList.add(attributeHref);
            }

        }
    }

    private void addToParentCategoryForCountry(List<String> parentCategoryUrlList, Elements aElementList) {
        for (Element element : aElementList) {
            if (element.attr("href").startsWith("http")) {
                parentCategoryUrlList.add(element.attr("href"));
            } else {
                String attributeHref = "http://www.trade-seafood.com/directory/seafood/country/" + element.attr("href");
                parentCategoryUrlList.add(attributeHref);
            }
        }
    }

    private List<String> extractTargetPageList(List<String> parentCategoryUrlList) throws Exception {
        List<String> targetPageList = new ArrayList<String>();
        for (String parentCategoryUrl : parentCategoryUrlList) {
            Document document = Jsoup.connect(parentCategoryUrl).get();
            Elements tableElementList = document.select("table");

            if (tableElementList.size() < 5) {
                System.out.println("tableElementList.size() < 5 :: " + parentCategoryUrl);
                continue;
            }

            Element tableElement = tableElementList.get(5);
            Elements bTagElementList = tableElement.select("b");
            if (bTagElementList.size() > 0) {
                Elements aTagElementList = bTagElementList.select("a");
                addToTargetPageList(targetPageList, aTagElementList);
            }
        }

        return targetPageList;
    }

    private void addToTargetPageList(List<String> targetPageList, Elements aTagElementList) {
        for (Element aTagElement : aTagElementList) {
            if (!aTagElement.text().equals("Click for Details")) {
                String targetUrl = "http://www.trade-seafood.com/directory/seafood/" + aTagElement.attr("href").replace("../", "");
                System.out.println(targetUrl);
                targetPageList.add(targetUrl);
            }
        }
    }

    private List<ExtractModel> extractForExcel(List<String> targetPageList) throws Exception {

        List<ExtractModel> extractModelList = new ArrayList<ExtractModel>();
        for (String targetPageUrl : targetPageList) {
            Document document = Jsoup.connect(targetPageUrl).get();
            Elements tableElementList = document.select("table");
            Element tableElement = tableElementList.get(5);

            ExtractModel extractModel = new ExtractModel();
            extractModel.setTargetUrl(targetPageUrl);
            Elements trElementList = tableElement.select("tr");
            for (Element trElement : trElementList) {
                Elements tdElementList = trElement.select("td");
                makeExtractModelInfo(extractModel, tdElementList);
            }
            System.out.println("타켓 페이지 :: " + extractModel.getTargetUrl());
            extractModelList.add(extractModel);
        }

        return extractModelList;

    }

    private void makeExtractModelInfo(ExtractModel extractModel, Elements tdElementList) {

        if (tdElementList.size() == 1) {

            if (tdElementList.get(0).text().startsWith("ABOUT")) {
                extractModel.setDescription(tdElementList.get(0).text());
            } else {
                extractModel.setCompanyName(tdElementList.get(0).text());
            }

        }

        if (tdElementList.size() == 2) {
            if (tdElementList.get(0).text().startsWith("Contact")) {
                extractModel.setContactName(tdElementList.get(1).text());
            }

            if (tdElementList.get(0).text().startsWith("Company")) {
                extractModel.setCompanyPosition(tdElementList.get(1).text());
            }

            if (tdElementList.get(0).text().startsWith("Address")) {
                extractModel.setAddress(tdElementList.get(1).text());
            }

            if (tdElementList.get(0).text().startsWith("Address Cont")) {
                extractModel.setAddressCount(tdElementList.get(1).text());
            }

            if (tdElementList.get(0).text().startsWith("City")) {
                extractModel.setCity(tdElementList.get(1).text());
            }

            if (tdElementList.get(0).text().startsWith("State")) {
                extractModel.setState(tdElementList.get(1).text());
            }

            if (tdElementList.get(0).text().startsWith("Postal")) {
                extractModel.setZipCode(tdElementList.get(1).text());
            }

            if (tdElementList.get(0).text().startsWith("Country")) {
                extractModel.setCountry(tdElementList.get(1).text());
            }

            if (tdElementList.get(0).text().startsWith("Tel")) {
                extractModel.setTel(tdElementList.get(1).text());
            }

            if (tdElementList.get(0).text().startsWith("Mobile")) {
                extractModel.setMobilePhone(tdElementList.get(1).text());
            }

            if (tdElementList.get(0).text().startsWith("Fax")) {
                extractModel.setFax(tdElementList.get(1).text());
            }

            if (tdElementList.get(0).text().startsWith("Skype")) {
                extractModel.setSkype(tdElementList.get(1).text());
            }

            if (tdElementList.get(0).text().startsWith("Email")) {
                extractModel.setEmail(tdElementList.get(1).text());
            }

            if (tdElementList.get(0).text().startsWith("Website")) {
                extractModel.setWebsite(tdElementList.get(1).text());
            }

        }
    }

    private void makeExcelFile(List<ExtractModel> extractModelList) {

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet xssfSheet = workbook.createSheet("mySheet");

        XSSFRow firstXSSRow = xssfSheet.createRow(1);
        firstXSSRow.createCell(1).setCellValue("Company Name");
        firstXSSRow.createCell(2).setCellValue("Contact Name");
        firstXSSRow.createCell(3).setCellValue("Company Position");
        firstXSSRow.createCell(4).setCellValue("Address");
        firstXSSRow.createCell(5).setCellValue("Address Cont.");
        firstXSSRow.createCell(6).setCellValue("City");
        firstXSSRow.createCell(7).setCellValue("State");
        firstXSSRow.createCell(8).setCellValue("Postal (Zip) Code");
        firstXSSRow.createCell(9).setCellValue("Country");
        firstXSSRow.createCell(10).setCellValue("Tel");
        firstXSSRow.createCell(11).setCellValue("Mobile (Cell) Phone");
        firstXSSRow.createCell(12).setCellValue("Fax");
        firstXSSRow.createCell(13).setCellValue("Skype");
        firstXSSRow.createCell(14).setCellValue("targetUrl");
        firstXSSRow.createCell(15).setCellValue("Email");
        firstXSSRow.createCell(16).setCellValue("Website");
        firstXSSRow.createCell(17).setCellValue("Description");

        int row = 2;
        for (ExtractModel extractModel : extractModelList) {
            XSSFRow xssfRow = xssfSheet.createRow(row);
            xssfRow.createCell(1).setCellValue(extractModel.getCompanyName());
            xssfRow.createCell(2).setCellValue(extractModel.getContactName());
            xssfRow.createCell(3).setCellValue(extractModel.getCompanyPosition());
            xssfRow.createCell(4).setCellValue(extractModel.getAddress());
            xssfRow.createCell(5).setCellValue(extractModel.getAddressCount());
            xssfRow.createCell(6).setCellValue(extractModel.getCity());
            xssfRow.createCell(7).setCellValue(extractModel.getState());
            xssfRow.createCell(8).setCellValue(extractModel.getZipCode());
            xssfRow.createCell(9).setCellValue(extractModel.getCountry());
            xssfRow.createCell(10).setCellValue(extractModel.getTel());
            xssfRow.createCell(11).setCellValue(extractModel.getMobilePhone());
            xssfRow.createCell(12).setCellValue(extractModel.getFax());
            xssfRow.createCell(13).setCellValue(extractModel.getSkype());
            xssfRow.createCell(14).setCellValue(extractModel.getTargetUrl());
            xssfRow.createCell(15).setCellValue(extractModel.getEmail());
            xssfRow.createCell(16).setCellValue(extractModel.getWebsite());
            xssfRow.createCell(17).setCellValue(extractModel.getDescription());
            row++;
        }

        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream("resultByCountry.xlsx");
            workbook.write(fileOutputStream);
            fileOutputStream.close();
            System.out.println("파일생성 완료");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<ExtractModel> makeDummy() {
        List<ExtractModel> extractModelList = new ArrayList<ExtractModel>();

        ExtractModel extractModel1 = new ExtractModel();

        extractModel1.setCompanyName("a");
        extractModel1.setContactName("b");
        extractModel1.setCompanyPosition("b");
        extractModel1.setAddress("b");
        extractModel1.setAddressCount("b");
        extractModel1.setCity("b");
        extractModel1.setState("b");
        extractModel1.setZipCode("b");
        extractModel1.setCountry("b");
        extractModel1.setTel("b");
        extractModel1.setMobilePhone("b");
        extractModel1.setFax("b");
        extractModel1.setSkype("b");
        extractModel1.setEmail("b");
        extractModel1.setWebsite("b");
        extractModel1.setDescription("b");

        extractModelList.add(extractModel1);

        ExtractModel extractModel = new ExtractModel();

        extractModel.setCompanyName("a");
        extractModel.setContactName("b");
        extractModel.setCompanyPosition("b");
        extractModel.setAddress("b");
        extractModel.setAddressCount("b");
        extractModel.setCity("b");
        extractModel.setState("b");
        extractModel.setZipCode("b");
        extractModel.setCountry("b");
        extractModel.setTel("b");
        extractModel.setMobilePhone("b");
        extractModel.setFax("b");
        extractModel.setSkype("b");
        extractModel.setEmail("b");
        extractModel.setWebsite("b");
        extractModel.setDescription("b");

        extractModelList.add(extractModel);
        return extractModelList;
    }
}
