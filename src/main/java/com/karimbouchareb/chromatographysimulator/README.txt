HOW RETENTION TIME IS CALCULATED =======================================================================================

    (1) Using Poole's 2019 paper, LSER constants for each compound and column were found
        "Gas chromatography system constant database over an extended temperature range for nine open-tubular columns"
        https://www.sciencedirect.com/science/article/abs/pii/S0021967319300433

    (2) Using the following equation, retention time can be computed (k = retention factor which yields retention time)

        logk = eE + sS + aA + bB + lL + C       (Equation 1)

        Intermolecular Attraction contributed by the COMPOUND(solute):
        e - excess dispersion interactions from the presence of polarizable electrons (including pi-pi interactions)
        s - dipole type interactions
        a - hydrogen bond acceptor interactions
        b - hydrogen bond donor interactions (effectively 0 because of multiplication; therefore not included)
        l - size independent cavity formation & dispersion interactions

        Intermolecular Attraction contributed by the COLUMN(system):
        E - excess dispersion interactions from the presence of polarizable electrons (including pi-pi interactions)
        S - dipole type interactions
        A - hydrogen bond acceptor interactions
        B - hydrogen bond donor interactions (ALWAYS zero for these columns so NOT included)
        L - size independent cavity formation & dispersion interactions
        C - a coefficient for curve correction to agree with experimental data

    (3) Importantly, the LSER constants for each COLUMN _change as a function of temperature_

    (4) Thus, retention time for a given peak must be dynamically calculated by multiplying static/unchanging LSER
        soluteConstants by the dynamic/changing/temperature-dependent systemConstants.

    (5) At the time the user hits the "Inject" button, a peak object is initialized. This object will have its
        column LSER constants dynamically calculated using polynomial functions. The functions return an LSER constant 
        value as a function of temperature. Polynomial functions and their coefficients were determined based on Poole's
        data for each LSER constant of each column. Explained one more time: each column has an eCurve, sCurve, aCurve, 
        lCurve, and cCurve where the independent variable is temperature and the dependent variable is the value of the
        corresponding column LSER constant.

    (6) If the temperature in the GC's simulated oven is changing, then the retention time should be recalculated when
        appropriate based on the new temperature.

        todo GOTTA DECIDE THIS!
    (7) The appropriate time to recalculate the retention factors for all peaks is ___<if (!currentlyEluting) every
        2 seconds??>___.


    (8) HoldUp Time estimated for each column using Reztek EZGC flow calculator (https://ez.restek.com/ezgc-mtfc).
        Assumed column temp of 180 degrees C (did not vary HoldUpTime as a function of temperature).



========================================================================================================================

HOW PEAK AREA IS CALCULATED ========================================================================================

    (1) Using De Saint Laumer's 2015 paper
        "Prediction of response factors for gas chromatography with flame ionization detection: Algorithm improvement,
        extension to silylated compounds, and application to the quantification of metabolites"
        https://pubmed.ncbi.nlm.nih.gov/26179324/

    (2) De Saint Laumer published equations for:

        A. Estimating combustion enthalpies for a given compound based on its molecular formula.
            combustionEnthalpy = 11.06 + 103.57nC + 21.85nH − 48.18nO + 7.46nN + 74.67nS − 23.57nF − 27.43nCl − 11.90nBr
            − 2.04nI
            (NOTE: nC = numberCarbons; nH = numberHydrogens; etc.)

        B. Molar Response Factor (areaPeakResponse / molesOfCompound)
            MRF = −0.0708 + (8.57*10^−4)combustionEnthalpy + (1.27*10^−1)nBenz + (6.18*10^−2)nBr
            (NOTE: nBenz = numberOfBenzeneRings; nBr = numberOfBromines)
    
    (3) The MRF values were calculated for all compounds in the chemicalData.csv using microsoft excel. 
        Peak area is shown graphically in this program in arbitrary units of area. These area units are _only_ 
        meaningful in that the peak areas for each injected mass of compound are accurate _relative to each other_.
        For example, the MRF of methyl octanoate is ~1. The MRF of n-hexadecane is ~2. If 8.0e-10 moles of methyl
        octanoate are injected on-column and 8.0e-10 moles of n-hexadecane are injected on-column, the peak area of the
        n-hexadecane will be twice the peak area of the methyl octanoate.

    (4) The peak areas are transformed to make them more pleasant to read. For example, consider a typical mass that 
        could be injected on-column such as 133 ng of methyl octanoate. Methyl octanoate has a molar response factor of 
        ~1. 133 ng of methyl octanoate is equal to 8.4e-10 moles of methyl octanoate. It is not very readable to say 
        that the area of this peak is 8.4e-10 arbitrary units of area. It is better to multiply the number of moles by a
        constant to yield, lets say, 248 arbitary units of area. A proportionalityConstant equaling 5.952e11 helps here.
        5.952e11(area units/moles) * 8.4e-10(moles) = 500 arbitrary area units.


========================================================================================================================

HOW PEAK SHAPE IS DETERMINED ===========================================================================================

    NOTE: In general, a peak has a <peakTailingIndex = 1.0> unless it should be biased upwards due to circumstances
    NOTE: In general, a peak has a <peakFrontingIndex = 1.0> unless it should be biased upwards due to circumstances
    NOTE: In general, a peak has a <peakBroadeningIndex = 1.0> unless it should be biased upwards due to circumstances

    WHEN SHOULD COMPOUNDS EXHIBIT SYMMETRICAL PEAK BROADENING ----------------------------------------------------------
        (1) POSSIBLE: Carrier Gas Flow Rate set too low (Consider Van Deemter Plot / Equation)
        (2) POSSIBLE: Poor Injection Technique (Slow injection)
        (3) Longer column: peaks should globally be broader compared to shorter columns (this is just related to
            retention Time).
        (4) RetentionTime: peaks symmetrical broadness should increase as a function of retention time (more time
            for diffusion to widen the band inside the column).
            (NOTE: this relationship was decided to be linearly proportional arbitrarily)

    WHEN SHOULD COMPOUNDS EXHIBIT PEAK FRONTING DUE TO OVERLOAD: CALCULATING EACH COMPOUND'S 'OVERLOAD MASS' IN NG -----
        (1) Sum the LSER constants of each compound: constantSum (NOTE: a HIGH constantSum means a compound is 
            generally likely to be HIGHLY retained in most columns). It is generally thought that highly retained
            compounds will "use up" more of the stationary phase interaction sites and will thus have lower masses that
            can be introduced on-column without overwhelming the loading capacity of the column and cause peak fronting.

        (2) Find mean & standard deviation of the set of constantSums (mean = 7.98, stdev = 3.20)

        (3) Find Z-score of each compound's constantSum

        (4) Calculate values for column "overloadMass_1" by: overloadMass_1 = 340-(42*zScore)
                (NOTE: overloadMass_1, overloadMass_05, overloadMass_025, and overloadMass_01 correspond to columns
                 with film thickness of 1.0 um, 0.5 um, 0.25 um, and 0.1 um respectively)

               a. This results in a distribution of values that __roughly__ matches Dean Rood's table described below.
                  The compound which has the highest LSER constantSum will be programmed to start overloading at a mass
                  above 200 ng on a 0.25 mm column with 1.0 um film thickness. The compound which has the lowest LSER
                  constantSum will be programmed to start overloading at a mass above 400 ng on a 0.25 mm column with
                  1.0 um film thickness. This distribution was chosen so that, when dividing the overloadMass_1 by 2
                  it results in a distribution of overloadMass_05 values that match Dean Rood's table. When dividing
                  overloadMass_1 by 4 it results in a distribution of overloadMass_025 values that match Dean Rood's
                  table, etc.

               b. This results in _generally highly retained_ compounds having an overload mass between 40 - 130 ng.
               c. This results in most compounds having an overload mass between 150 - 250 ng
               d. This results in compounds which are _generally not highly retained_ having an overload mass which
                  approaches 300 ng.

        (5) https://www.chromatographyonline.com/view/how-much-sample-can-i-put-on-my-gc-column- is a highly informative
            article. Ultimately, a decision was made, based on their findings that peak overload is a highly 
            unpredictable phenomenon that occurs due to a variety of not-yet understood mechanisms, to reduce the scheme
            to that described in steps 1-4. Dean Rood's book, "The Troubleshooting and Maintenance Guide for Gas 
            Chromatographers" is mentioned in this article (the 2007 revised edition is different title than the 1991
            version cited in the article). It contains a table on page 29 (NOTE: values for different column diameters
            and film thicknesses that do not apply to the columns simulated in this program are omitted):

                                        "Approximate column capacities"

                                                        Column Diameter
                                                    0.25 mm         0.32 mm
                                Film Thickness      
                                     0.10           25-50 ng        35-75 ng       
                                     0.25           50-100 ng       75-125 ng
                                     0.5            100-200 ng      125-250 ng
                                     1.0            200-[400]* ng      250-500 ng
            
                    The Table is Captioned: "For similar polarity stationary phases and compounds. 
                                       Capacities reported as ng _per compound_"
                    *: range fudged up from 300 ng to 400 ng for easy rough math (see above).
        
            It seems, based on the table's caption, that "For similar polarity stationary phases and compounds" could be   
            considered to mean those compound/column pairs which are highly retained (i.e. have a high retentionFactor
            or have a high sum of their multiplied LSER constants). Thus, it is a reasonable, though innaccurate,
            approximation to just sum the LSER constants of all the compounds and label the compound with the highest
            constantSum as having an overload mass of 25 ng on a 0.25 mm column with 0.10 film thickness (e.g. the HP-5)
            and an overload mass of 200 ng on a 0.25 mm column with a 1.0 film thickness (e.g. SPB-Octyl).
            
            This method of estimating overload by compound could be implemented someday, but for now, the much lazier 
            process described above will be used haha.

    todo MORE TO DO HERE!!!!
   WHEN SHOULD COMPOUNDS EXHIBIT PEAK TAILING --------------------------------------------------------------------------
        (1) POSSIBLE: If a column is placed into the oven, perhaps the chemist/technician didn't cut the column
            well with their sapphire scribe. Thus, a random chance of 20% for every column switch may result in a
            Column objects boolean isPoorlyCut being set to true. This will impose a global peak tailing "debuff" on
            all peaks generated inside this column.
        (2) POSSIBLE: An arbitrary rule of thumb for increasing the peak tailing index could be that if the oven temp
            is not far enough above the boiling point, we should increase the peakTailing index

        (3) POSSIBLE: Peak tailing due to low carrier gas velocity

        ...


        ...

        ...
        ...
        ...
        ...

        ...
        ...

========================================================================================================================

LIMITATIONS & ASSUMPTIONS ==============================================================================================
    
    (1) All simulated injections have an integer value volume between 1 uL - 10 uL.
    (2) 
    (3) Max concentration of analytes in an injection is arbitrarily set at 10%chemicals are assumed to be miscible.  




========================================================================================================================

WHERE IS THE DATA FROM =================================================================================================

    (1) Combustion enthalpy data was calculated using De Saint Laumer's equations operating on the molecular formula
        data obtained from the NIST chemistry Web Book
    (2) MRF data was calculated using De Saint Laumer's equations and combustion enthalpy data
    (3) Benzene Ring count was counted manually
    (4) Overload Mass calculated as explained above
    (5) CAS numbers obtained from NIST chemistry webbook
    (6) LSER Constants obtained from Poole's 2019 paper explained above
    (7) Polynomial equations for determing the LSER constants of columns in a temperature dependent manner were
        calculated using the PolynomialFitting class in this package using Apache Commons 3 Math library and ChatGPT.
    (8) HoldupTime for each column calculated using https://ez.restek.com/ezgc-mtfc
    (9) Chromatography online was referenced for mass overload and column capacity questions as well

========================================================================================================================