function updateOrders() {
    let currencyToSell = window.tradingData.activeCurrencyToSell;
    let currencyToBuy = window.tradingData.activeCurrencyToBuy;

    $.ajaxSetup({ cache: false })
    $.get(`/order/${currencyToSell}/${currencyToBuy}/table`, function(data) {
        let sellOrdersHtml = '';
        let buyOrdersHtml = '';

        // Формируем HTML для всех строк таблицы продажи
        data.sellOrders.forEach(order => {
            sellOrdersHtml += `<tr><td>${Number(order.rate).toFixed(6)}</td><td>${Number(order.summedAmount).toFixed(2)}</td></tr>`;
        });

        // Формируем HTML для всех строк таблицы покупки
        data.buyOrders.forEach(order => {
            buyOrdersHtml += `<tr><td>${Number(order.rate).toFixed(6)}</td><td>${Number(order.summedAmount).toFixed(2)}</td></tr>`;
        });

        // Обновляем DOM только один раз для каждой таблицы
        $(".sell-orders table tbody").html(sellOrdersHtml);
        $(".buy-orders table tbody").html(buyOrdersHtml);

        let orderType = window.tradingData.orderType;
        if (orderType === "MARKET") {
            let rate = Number(data.sellOrders[0].rate).toFixed(6);
            $("#buy-rate").val(rate);
        }
    });
}

function updateBuyBalance() {
    let currencyToSell = window.tradingData.activeCurrencyToSell;
    $.get(`/trading/${currencyToSell}/balance`, function(balance) {
        $("#you-give-away").val(balance);
        updateCalculations('you-give-away');
    });
}

function updateCalculations(sourceElementId = 'you-give-away') {
    const youGet = $("#you-get");
    const youGiveAway = $("#you-give-away");
    const buyRate = $("#buy-rate");
    const commissionPercent = $("#buy-commission-percent");
    const commissionAmount = $("#buy-commission-amount");

    // Преобразуем значения в числа
    let youGetValue = parseFloat(youGet.val()) || 0;
    let youGiveAwayValue = parseFloat(youGiveAway.val()) || 0;
    let buyRateValue = parseFloat(buyRate.val()) || 0;
    let commissionPercentValue = parseFloat(commissionPercent.val()) || 0;

    if (sourceElementId === 'you-give-away') {
        // Если изменено "Вы отдаете", то пересчитываем "Вы получаете"
        if (buyRateValue > 0) {
            // youGiveAway - 100%
            // youGet - 99.9%
            if (window.tradingData.ratesNormalized) {
                youGetValue = (youGiveAwayValue - youGiveAwayValue * commissionPercentValue / 100) * buyRateValue;
            } else {
                youGetValue = (youGiveAwayValue - youGiveAwayValue * commissionPercentValue / 100) / buyRateValue;
            }
            youGet.val(youGetValue.toFixed(2));
        }
    } else if (sourceElementId === 'you-get') {
        // Если изменено "Вы получаете", то пересчитываем "Вы отдаете"
        // youGet - 99.9%
        // youGiveAway - 100%
        if (window.tradingData.ratesNormalized) {
            youGiveAwayValue = youGetValue / (100 - commissionPercentValue) * 100 / buyRateValue;
        } else {
            youGiveAwayValue = youGetValue / (100 - commissionPercentValue) * 100 * buyRateValue;
        }
        youGiveAway.val(youGiveAwayValue.toFixed(2));
    } else if (sourceElementId === 'buy-rate') {
        // Если изменен курс, то пересчитываем "Вы получаете" на основе "Вы отдаете"
        if (youGiveAwayValue > 0 && buyRateValue > 0) {
            youGetValue = (youGiveAwayValue - youGiveAwayValue * commissionPercentValue / 100) * buyRateValue;
        } else {
            youGetValue = (youGiveAwayValue - youGiveAwayValue * commissionPercentValue / 100) / buyRateValue;
        }
        youGet.val(youGetValue.toFixed(2));
    }

    // Рассчитываем комиссию
    const commissionAmountValue = youGiveAwayValue * commissionPercentValue / 100;
    commissionAmount.val(commissionAmountValue.toFixed(2));
}

// Функция для открытия модального окна
function openModal() {
    const modal = $('#confirmationModal');
    modal.css('display', 'block');

    // Заполняем данные в модальном окне
    $('#modal-order-type').text(window.tradingData.orderType === 'MARKET' ? 'По рынку' : 'Фикс. цена');
    $('#modal-you-give-away').text($('#you-give-away').val() + ' ' + window.tradingData.activeCurrencyToSell);
    $('#modal-rate').text($('#buy-rate').val() + ' ' + window.tradingData.activeCurrencyToSell);
    $('#modal-you-get').text($('#you-get').val() + ' ' + window.tradingData.activeCurrencyToBuy);
}

// Функция для закрытия модального окна
function closeModal() {
    const modal = $('#confirmationModal');
    modal.css('display', 'none');
}

$(document).ready(function() {
    $("#you-get, #you-give-away, #buy-rate").on('input', function() {
        updateCalculations($(this).attr('id'))
    });

    setInterval(updateOrders, 2000);

    $(".buy-section .max-btn").click(function() {
        updateBuyBalance('you-give-away')
    });

    // Обработчик события для кнопки "ОБМЕНЯТЬ"
    $('.exchange-btn').click(openModal);

    // Обработчик события для закрытия модального окна
    $('.close').click(closeModal);

    // Обработчик события для кнопки "ПОДТВЕРДИТЬ"
    $('#confirmExchange').on('click', function() {
        $('#amountToSell').val(parseFloat($('#you-give-away').val()));
        $('#orderType').val(window.tradingData.orderType);

        if (window.tradingData.orderType !== 'LIMIT') {
            $('#rate').val(parseFloat($('#buy-rate').val()));
        }

        //$('#buy-rate'
        console.log("$(\'#buy-rate\'" + $('#buy-rate'))
        console.log("$('#you-give-away')" + $('#you-give-away'))
        $('#exchangeForm').submit();
    });

    // Закрытие модального окна при клике вне его области
    $(window).click(function(event) {
        const modal = $('#confirmationModal');
        if (event.target === modal[0]) {
            closeModal();
        }
    });
});
