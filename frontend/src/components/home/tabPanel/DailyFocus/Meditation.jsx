import HomeStyles from "../../../../pages/Home/Home.module.css"; // koristi isti CSS kao tabPanel
import styles from "../../../../pages/Home/Home.module.css";

import { useState, useEffect} from "react";
import { useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";

export default function DailyFocus({user}) { 

    return(
        "ovdje ce stajat jedan video meditacije koji ce random mijenjat"
    );
}