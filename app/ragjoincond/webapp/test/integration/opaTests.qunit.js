sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'ragjoincond/test/integration/FirstJourney',
		'ragjoincond/test/integration/pages/RagJoinCondList',
		'ragjoincond/test/integration/pages/RagJoinCondObjectPage'
    ],
    function(JourneyRunner, opaJourney, RagJoinCondList, RagJoinCondObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('ragjoincond') + '/index.html'
        });

       
        JourneyRunner.run(
            {
                pages: { 
					onTheRagJoinCondList: RagJoinCondList,
					onTheRagJoinCondObjectPage: RagJoinCondObjectPage
                }
            },
            opaJourney.run
        );
    }
);