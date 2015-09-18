package se.gustavkarlsson.aurora_notifier.web_service.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.*;

public abstract class AggregateProvider<T> implements Provider<T> {

	private static final Logger logger = LoggerFactory.getLogger(AggregateProvider.class);

	private final List<Provider<T>> providers = new ArrayList<>();

	public AggregateProvider(Collection<Provider<T>> providers) {
		checkNotNull(providers);
		checkArgument(!providers.contains(null), "One provider is null");
		this.providers.addAll(providers);
	}

	public AggregateProvider() {
	}

	public void addProvider(Provider<T> provider) {
		providers.add(checkNotNull(provider));
	}

	protected List<T> getValues() throws ProviderException {
		checkState(!providers.isEmpty(), "No providers to get value from");
		List<T> values = new ArrayList<>();
		for (Provider<T> provider : providers) {
			try {
				values.add(provider.getValue());
			} catch (ProviderException e) {
				logger.warn("Provider failed. Continuing...", e);
			}
		}
		if (values.isEmpty()) {
			throw new ProviderException("No provider successfully returned a value");
		}
		return values;
	}
}