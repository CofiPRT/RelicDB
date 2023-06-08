package ro.cofi.relicdb;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DBScraper {

    private static final String URL_ROOT = "https://genshin.gg";
    private static final String URL_STAR_RAIL = URL_ROOT + "/star-rail";

    private static final String MAIN_STAT_BODY = "Body";
    private static final String MAIN_STAT_FEET = "Feet";
    private static final String MAIN_STAT_PLANAR_SPHERE = "Planar Sphere";
    private static final String MAIN_STAT_LINK_ROPE = "Link Rope";
    private static final Pattern MAIN_STAT_PATTERN = Pattern.compile("<b>(.+)</b>\\s*(.*)");

    public JsonObject scrape() throws IOException {
        JsonObject rootObject = new JsonObject();

        rootObject.add("characters", scrapeCharacters());

        return rootObject;
    }

    private JsonArray scrapeCharacters() throws IOException {
        JsonArray characterArray = new JsonArray();

        Document doc = Jsoup.connect(URL_STAR_RAIL).get();

        Elements characters = selectNonEmptyElements(
            doc, ".character-portrait", "Could not find the characterList"
        );

        for (Element character : characters)
            characterArray.add(scrapeCharacter(character));

        return characterArray;
    }

    private JsonObject scrapeCharacter(Element character) throws IOException {
        JsonObject characterObject = new JsonObject();

        Element characterName = selectNonNullElement(
            character, ".character-name", "Could not find the character name"
        );

        characterObject.addProperty("name", characterName.text());

        String characterURL = character.absUrl("href");

        characterObject.addProperty("url", characterURL);

        Document doc = Jsoup.connect(characterURL).get();

        Elements buildSections = doc.select(".character-info-build-section");

        characterObject.add("relics", scrapeRelics(buildSections));
        characterObject.add("ornaments", scrapeOrnaments(buildSections));
        characterObject.add("mainStats", scrapeMainStats(buildSections));
        characterObject.add("subStats", scrapeSubStats(buildSections));

        return characterObject;
    }

    private JsonArray scrapeRelics(Elements buildSections) throws IOException {
        return scrapeWeapons(buildSections, "Best Relics", "relic", true);
    }

    private JsonArray scrapeOrnaments(Elements buildSections) throws IOException {
        return scrapeWeapons(buildSections, "Best Ornaments", "ornament", false);
    }

    private JsonArray scrapeWeapons(
        Elements buildSections, String title, String type, boolean allowMultipleSets
    ) throws IOException {
        JsonArray weaponOptionsResult = new JsonArray();

        Element weaponsSection = selectSection(buildSections, title);

        Elements weaponOptions = selectNonEmptyElements(
            weaponsSection, ".character-info-weapon", String.format("Could not find %s options", type)
        );

        for (Element weaponOption : weaponOptions)
            weaponOptionsResult.add(scrapeWeaponOption(weaponOption, type, allowMultipleSets));

        return weaponOptionsResult;
    }

    private JsonObject scrapeWeaponOption(
        Element relicOption, String type, boolean allowMultipleSets
    ) throws IOException {
        JsonObject weaponOptionObject = new JsonObject();

        Element rankElement = selectNonNullElement(
            relicOption, ".character-info-weapon-rank", String.format("Could not find %s rank", type)
        );

        weaponOptionObject.addProperty("rank", Integer.parseInt(rankElement.text()));

        Elements relicSets = selectNonEmptyElements(
            relicOption, ".character-info-weapon-content", String.format("Could not find %s sets", type)
        );

        if (!allowMultipleSets && relicSets.size() > 1)
            throw new IOException(String.format("Found multiple %s sets%n%s", type, relicOption));

        JsonArray weaponSetsArray = new JsonArray();

        for (Element weaponSet : relicSets)
            weaponSetsArray.add(scrapeWeaponSet(weaponSet, type));

        weaponOptionObject.add("options", weaponSetsArray);

        return weaponOptionObject;
    }

    private JsonObject scrapeWeaponSet(Element weaponSet, String type) throws IOException {
        JsonObject weaponSetObject = new JsonObject();

        Element nameElement = selectNonNullElement(
            weaponSet, ".character-info-weapon-name", String.format("Could not find %s set name", type)
        );

        weaponSetObject.addProperty("name", nameElement.text());

        Element countElement = selectNonNullElement(
            weaponSet, ".character-info-weapon-count", String.format("Could not find %s set count", type)
        );

        weaponSetObject.addProperty("count", Integer.parseInt(countElement.text()));

        return weaponSetObject;
    }

    private JsonObject scrapeMainStats(Elements buildSections) throws IOException {
        JsonObject mainStatsObject = new JsonObject();

        Element mainStatsSection = selectSection(buildSections, "Best Stats");

        Elements mainStats = selectNonEmptyElements(
            mainStatsSection, ".character-info-stats-item", "Could not find main stats"
        );

        mainStatsObject.addProperty("body", scrapeMainStat(mainStats, MAIN_STAT_BODY));
        mainStatsObject.addProperty("feet", scrapeMainStat(mainStats, MAIN_STAT_FEET));
        mainStatsObject.addProperty("planarSphere", scrapeMainStat(mainStats, MAIN_STAT_PLANAR_SPHERE));
        mainStatsObject.addProperty("linkRope", scrapeMainStat(mainStats, MAIN_STAT_LINK_ROPE));

        return mainStatsObject;
    }

    private String scrapeMainStat(Elements mainStats, String stat) throws IOException {
        Element mainStat = mainStats.stream()
            .filter(element -> element.text().contains(stat))
            .findFirst()
            .orElseThrow(() -> new IOException(
                String.format("Could not find the \"%s\" main stat%n%s", stat, mainStats.html())
            ));

        // the element is of the form
        // <b>[stat]:</b> [value]
        // retrieve the value
        Matcher matcher = MAIN_STAT_PATTERN.matcher(mainStat.html());

        if (!matcher.matches())
            throw new IOException(String.format("Could not parse the \"%s\" main stat%n%s", stat, mainStat.html()));

        return matcher.group(2);
    }

    private JsonArray scrapeSubStats(Elements buildSections) throws IOException {
        JsonArray subStatsArray = new JsonArray();

        Element subStatsSection = selectSection(buildSections, "Best Substats");

        Elements subStats = selectNonEmptyElements(
            subStatsSection, ".character-info-stats-item", "Could not find substats"
        );

        for (Element subStat : subStats)
            subStatsArray.add(subStat.text());

        return subStatsArray;
    }

    private Element selectSection(Elements buildSections, String title) throws IOException {
        return buildSections.stream()
            .filter(element -> {
                Element titleElement = element.selectFirst(".character-info-build-section-title");
                if (titleElement == null)
                    return false;

                return titleElement.text().contains(title);
            })
            .findFirst()
            .orElseThrow(() -> new IOException(
                String.format("Could not find the \"%s\" section%n%s", title, buildSections.html())
            ));
    }

    private Elements selectNonEmptyElements(Element element, String selector, String errorMessage) throws IOException {
        Elements elements = element.select(selector);
        if (elements.isEmpty())
            throw new IOException(String.format("%s%n%s", errorMessage, element.html()));

        return elements;
    }

    private Element selectNonNullElement(Element element, String selector, String errorMessage) throws IOException {
        Element selectedElement = element.selectFirst(selector);
        if (selectedElement == null)
            throw new IOException(String.format("%s%n%s", errorMessage, element.html()));

        return selectedElement;
    }

}
