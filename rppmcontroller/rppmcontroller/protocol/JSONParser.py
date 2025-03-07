import json as json
from rppmcontroller.protocol.JSONOutput import JSONOutput

import logging

class JSONParser:

    #TODO this function name is misleading , must be changed ...

    @staticmethod
    def parse(obj) -> str:
        s = json.dumps(obj.to_dict(), default=str)
        logging.debug(s)
        return s


#only for testing
# if __name__ == "__main__":
#     r = Robot("robb1")
#     a = MachineStatusRequestAnswer("STATUSANSWER", 2, r.request([RequestedParameter.REFERNCESWITCHCLAW]))
#     #a = MachineStatusRequestAnswer("STATUSANSWER", 2, r.request([RequestedParameter.ALL]))
#     j = JSONOutput("name", 12343.234, a)
#     print(j)
#     JSONParser.parse(j)
