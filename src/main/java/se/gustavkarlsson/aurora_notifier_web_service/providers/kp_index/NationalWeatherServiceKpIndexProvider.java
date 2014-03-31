package se.gustavkarlsson.aurora_notifier_web_service.providers.kp_index;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import se.gustavkarlsson.aurora_notifier_web_service.domain.KpIndexHolder;
import se.gustavkarlsson.aurora_notifier_web_service.providers.Provider;
import se.gustavkarlsson.aurora_notifier_web_service.providers.ProviderException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public class NationalWeatherServiceKpIndexProvider implements Provider<KpIndexHolder> {

	private static final String CHARSET = "UTF-8";
	private static final String URL = "http://www.swpc.noaa.gov/wingkp/wingkp_list.txt";

	private final Timer getValueTimer;
	private final Meter errorsMeter;

	public NationalWeatherServiceKpIndexProvider(MetricRegistry metrics) {
		getValueTimer = createGetValueTimer(metrics);
		errorsMeter = createErrorsMeter(metrics);
	}

	private Timer createGetValueTimer(MetricRegistry metrics) {
		return metrics.timer(MetricRegistry.name(getClass(), "getValue"));
	}

	private Meter createErrorsMeter(MetricRegistry metrics) {
		return metrics.meter(MetricRegistry.name(getClass(), "errors"));
	}

	@Override
	public KpIndexHolder getValue() throws ProviderException {
		try (Timer.Context timerContext = getValueTimer.time()) {
			URL url = new URL(URL);
			String urlContent = getUrlContent(url);
			float kpIndexValue = parseKpIndex(urlContent);
			long timestampMillis = System.currentTimeMillis();
			KpIndexHolder kpIndexHolder = new KpIndexHolder(kpIndexValue, timestampMillis);
			return kpIndexHolder;
		} catch (IOException e) {
			errorsMeter.mark();
			throw new ProviderException(e);
		}
	}

	private String getUrlContent(URL url) throws IOException {
		InputStream stream = url.openStream();
		Scanner scanner = new Scanner(stream, CHARSET);
		scanner.useDelimiter("\\A");
		String content = scanner.hasNext() ? scanner.next() : "";
		scanner.close();
		return content;
	}

	private float parseKpIndex(final String content) throws ProviderException {
		try {
			final String[] lines = content.split("\\n");
			final String lastLine = lines[lines.length - 1];
			final String[] lastLineSplit = lastLine.split("\\s+");
			final String kpIndexString = lastLineSplit[9];
			final float kpIndex = Float.parseFloat(kpIndexString);
			return kpIndex;
		} catch (RuntimeException e) {
			throw new ProviderException(e);
		}
	}
}