sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'ragwithjoincond/test/integration/FirstJourney',
		'ragwithjoincond/test/integration/pages/RagJoinCondList',
		'ragwithjoincond/test/integration/pages/RagJoinCondObjectPage'
    ],
    function(JourneyRunner, opaJourney, RagJoinCondList, RagJoinCondObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('ragwithjoincond') + '/index.html'
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