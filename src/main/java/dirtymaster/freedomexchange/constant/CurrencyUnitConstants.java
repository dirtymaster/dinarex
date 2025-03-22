package dirtymaster.freedomexchange.constant;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.List;

public class CurrencyUnitConstants {
    public static final CurrencyUnit EUR = Monetary.getCurrency("EUR");
    public static final CurrencyUnit RSD = Monetary.getCurrency("RSD");
    public static final CurrencyUnit RUB = Monetary.getCurrency("RUB");

    public static final List<CurrencyUnit> availableCurrencies = List.of(EUR, RSD, RUB);
}
