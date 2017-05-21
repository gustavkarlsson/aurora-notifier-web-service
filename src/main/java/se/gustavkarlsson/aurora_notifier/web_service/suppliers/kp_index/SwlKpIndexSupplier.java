package se.gustavkarlsson.aurora_notifier.web_service.suppliers.kp_index;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import se.gustavkarlsson.aurora_notifier.web_service.security.SslSecurityOverrider;

import java.net.URL;
import java.util.regex.Pattern;

public class SwlKpIndexSupplier extends WebBasedKpIndexSupplier {
	private static final String URL = "https://www.spaceweatherlive.com/en/auroral-activity/the-kp-index";
	private static final String CSS_PATH = "body > div.body > div > div > div.col-sx-12.col-sm-8 > h5 > a:nth-child(1)";
	private static final Pattern PK_INDEX_PATTERN = Pattern.compile("(-|0\\+?|[1-8](-|\\+)?|9-?)");

	static {
		SslSecurityOverrider.override();
	}

	@Inject
	SwlKpIndexSupplier(MetricRegistry metrics) {
		this(metrics, parseUrl(URL));
	}

	SwlKpIndexSupplier(MetricRegistry metrics, URL url) {
		super(metrics, url);
	}

	@Override
	protected float parseKpIndex(final String urlContent) {
		Document document = Jsoup.parse(urlContent);
		Elements elements = document.select(CSS_PATH);
		String text = elements.text();
		if (!PK_INDEX_PATTERN.matcher(text).matches()) {
			throw new IllegalArgumentException("Invalid Kp index: '" + text + "'");
		}
		float whole = parseWhole(text);
		float extra = parseExtra(text);
		return whole + extra;
	}

	private static float parseWhole(String text) {
		if (text.equals("-")) {
			return 0;
		}
		String firstChar = text.substring(0, 1);
		return Float.valueOf(firstChar);
	}

	private static float parseExtra(String text) {
		String ending = text.substring(1);
		switch (ending) {
			case "-":
				return -0.33f;
			case "+":
				return 0.33f;
			default:
				return 0;
		}
	}
}
