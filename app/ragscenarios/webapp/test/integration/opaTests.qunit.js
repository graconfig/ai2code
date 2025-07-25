sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'ragscenarios/test/integration/FirstJourney',
		'ragscenarios/test/integration/pages/BusinessScenariosList',
		'ragscenarios/test/integration/pages/BusinessScenariosObjectPage'
    ],
    function(JourneyRunner, opaJourney, BusinessScenariosList, BusinessScenariosObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('ragscenarios') + '/index.html'
        });

       
        JourneyRunner.run(
            {
                pages: { 
					onTheBusinessScenariosList: BusinessScenariosList,
					onTheBusinessScenariosObjectPage: BusinessScenariosObjectPage
                }
            },
            opaJourney.run
        );
    }
);